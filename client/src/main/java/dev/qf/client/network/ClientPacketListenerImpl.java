package dev.qf.client.network;

import common.Menu;
import common.Order;
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
    private final SerializableHandler handler;
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

    /*
    서버로부터 어떤 종류의 데이터(options, menus, orders 등)를 받든 상관없이
    데이터를 업데이트한 후 finally 블록에서 무조건 해당 레지스트리를 동결시킴.
    이 때문에 초기 데이터 동기화 과정에서 orders 데이터가 들어올 때 OrderRegistry
    동결 처리. 그 이후에 사용자가 주문 상태를 변경하면, onOrderStatusChanged
    메소드가 호출되어 이미 동결된 OrderRegistry에 데이터를 추가하려고 시도하면서
    Registry is frozen 예외가 발생.
    */

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
            return;
        }
        try {
            registry.unfreeze();
            registry.clear();
            registry.addAll(packet.data());
        } finally {
            registry.freeze();
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

        /*
        서버로부터 받은 최신 주문 정보(packet.order())를 사용하지 않고
        로컬에 저장된 옛날 주문 정보를 다시 addOrder 메소드에 전달.
        주문 상태가 업데이트되지 않고 이전 상태로 덮어쓰기되는 오류가 있음.
         */

    @Override
    public void onOrderStatusChanged(OrderUpdatedS2CPacket packet) {
        Order updatedOrder = packet.order();
        Order existingOrder = RegistryManager.ORDERS.get(updatedOrder.orderId());

        if (existingOrder != null && !existingOrder.cart().equals(updatedOrder.cart())) {
            logger.warn("Order data seems different. Overriding. Before: {}, After: {}", existingOrder, updatedOrder);
        }
        try {
            RegistryManager.ORDERS.unfreeze();
            RegistryManager.ORDERS.addOrder(updatedOrder);
        } finally {
            RegistryManager.ORDERS.freeze();
        }

        logger.info("Order ID {} has been updated to status: {}", updatedOrder.orderId(), updatedOrder.status());
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
