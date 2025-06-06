package dev.qf.client.network;

import common.Menu;
import common.network.SynchronizeData;
import common.network.encryption.NetworkEncryptionUtils;
import common.network.handler.SerializableHandler;
import common.network.handler.listener.ClientPacketListener;
import common.network.packet.*;
import common.registry.Registry;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import dev.qf.client.event.DataReceivedEvent;
import org.slf4j.Logger;
import common.Category;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class ClientPacketListenerImpl implements ClientPacketListener {
    private SerializableHandler handler;
    private final Logger logger = KioskLoggerFactory.getLogger();

    public ClientPacketListenerImpl(SerializableHandler channel) {
        this.handler = channel;
    }

    /**
     * Client가 처음으로 Server 와 연결 된 이후, Client 가 Server에게 HandShakeC2SPacket 을 보낼 떄 서버가 callback 으로 전달하는 패킷이다.<br>
     * 해당 패킷에는 서버의 public 키가 암호화 된 상태로 들어있다. <br>
     * 또한 nonce 역시 존재한다. Client 에서는 nonce를 사용할 일이 딱히 없으나, 클라이언트가 서버로 같은 nonce 값을 보내지 않으면 연결이 거절된다.
     * @param packet
     */
    @Override
    public void onHello(HelloS2CPacket packet) {
        SecretKey secretKey = NetworkEncryptionUtils.generateSecretKey();
        PublicKey publicKey = packet.getPublicKey();

        Cipher encrpytionCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secretKey);
        Cipher decryptionCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.DECRYPT_MODE, secretKey);

        KeyC2SPacket secretPacket = new KeyC2SPacket(secretKey, publicKey, packet.nonce());
        logger.info("Client public key sent");
        this.handler.send(secretPacket);
        this.handler.encrypt(encrpytionCipher, decryptionCipher);
    }

    @Override
    public void onReceivedData(UpdateDataPacket.ResponseDataS2CPacket packet) {
        if (!this.handler.isEncrypted()) {
            throw new IllegalStateException("Client is not encrypted");
        }
        logger.info("Received data : {}", packet.registryId());
        logger.info("data info : {}", packet);
        Registry<? extends SynchronizeData<?>> registry =  RegistryManager.getAsId(packet.registryId());
        if (registry == null) {
            logger.error("Received data from unknown registry : {}", packet.registryId());
        }

        // 메뉴 데이터인 경우 중복 제거
        if ("menus".equals(packet.registryId())) {
            logger.info("Processing menus data with duplicate removal (original: {} items)", packet.data().size());
            logger.info("Registry size before processing: {}", registry.getAll().size());

            Map<String, SynchronizeData<?>> uniqueMenus = new LinkedHashMap<>();

            for (SynchronizeData<?> data : packet.data()) {
                Menu menu = (Menu) data;
                String menuId = menu.id();

                if (uniqueMenus.containsKey(menuId)) {
                    Menu previousMenu = (Menu) uniqueMenus.get(menuId);
                    logger.warn("Duplicate menu detected - ID: {}, Name: {}", menuId, menu.name());
                    logger.warn("Previous soldOut: {}, New soldOut: {}", previousMenu.soldOut(), menu.soldOut());
                    logger.info("Replacing with newer data");
                } else {
                    logger.debug("Added unique menu: {} ({}), soldOut: {}", menu.name(), menuId, menu.soldOut());
                }


                uniqueMenus.put(menuId, data);
            }

            // Registry 초기화
            try {
                List<SynchronizeData<?>> existingMenus = new ArrayList<>(registry.getAll());
                for (SynchronizeData<?> existingData : existingMenus) {
                    Menu existingMenu = (Menu) existingData;
                    registry.remove(existingMenu.id());
                }
                logger.info("Registry cleared - removed {} existing menus", existingMenus.size());
            } catch (Exception e) {
                logger.warn("Cannot remove existing items: {}", e.getMessage());
            }

            for (SynchronizeData<?> data : uniqueMenus.values()) {
                Menu menu = (Menu) data;
                registry.add(menu.id(), data);
                logger.debug("Added to registry: {} - soldOut: {}", menu.name(), menu.soldOut());
            }

            logger.info("Menus registry updated with {} unique items (original: {} items)",
                    uniqueMenus.size(), packet.data().size());
            logger.info("Registry size after processing: {}", registry.getAll().size());

            for (SynchronizeData<?> finalData : registry.getAll()) {
                Menu finalMenu = (Menu) finalData;
                logger.info("Final menu state: {} ({}) - soldOut: {}",
                        finalMenu.name(), finalMenu.id(), finalMenu.soldOut());
            }
        }
        else if ("categories".equals(packet.registryId())) {
            logger.info("Processing categories data with duplicate removal (original: {} items)", packet.data().size());
            logger.info("Registry size before processing: {}", registry.getAll().size());

            Map<String, SynchronizeData<?>> uniqueData = new LinkedHashMap<>();

            for (SynchronizeData<?> data : packet.data()) {
                Category category = (Category) data;
                String id = category.cateId();

                if (uniqueData.containsKey(id)) {
                    logger.warn("Duplicate category detected and replaced: {} ({})",
                            category.cateName(), id);
                } else {
                    logger.info("Added unique category: {} ({})",
                            category.cateName(), id);
                }
                uniqueData.put(id, data);
            }

            try {
                for (SynchronizeData<?> existingData : registry.getAll()) {
                    Category existingCategory = (Category) existingData;
                    registry.remove(existingCategory.cateId());
                }
                logger.info("Registry cleared by removing all existing items");
            } catch (Exception e) {
                logger.warn("Cannot remove existing items: {}", e.getMessage());
            }

            for (SynchronizeData<?> data : uniqueData.values()) {
                Category category = (Category) data;
                registry.add(category.cateId(), data);
            }

            logger.info("Categories registry updated with {} unique items (original: {} items)",
                    uniqueData.size(), packet.data().size());
            logger.info("Registry size after processing: {}", registry.getAll().size());
        } else {
            registry.addAll(packet.data());
        }

        DataReceivedEvent.EVENT.invoker().onRegistryChanged(this.handler, registry);
    }

    @Override
    public void onEncryptCompleted(EncryptCompleteS2CPacket packet) {
        handler.send(new UpdateDataPacket.RequestDataC2SPacket("all"));
    }

    @Override
    public void onVerifyPurchaseResult(VerifyPurchasePackets.VerifyPurchaseResultS2CPacket packet) {
        if (!this.handler.isEncrypted()) {
            throw new IllegalStateException("Client is not encrypted");
        }
    }

    @Override
    public SidedPacket.Side getSide() {
        return SidedPacket.Side.CLIENT;
    }

    @Override
    public SerializableHandler getHandler() {
        return this.handler;
    }
}
