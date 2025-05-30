package dev.qf.server.network;

import com.google.common.primitives.Ints;
import common.network.SynchronizeData;
import common.network.encryption.NetworkEncryptionUtils;
import common.network.packet.*;
import common.registry.Registry;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import common.network.handler.SerializableHandler;
import common.network.handler.listener.ServerPacketListener;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.util.List;
import java.util.random.RandomGenerator;

@ApiStatus.Internal
public class ServerPacketListenerImpl implements ServerPacketListener {
    private final SerializableHandler handler;
    private final Logger logger = KioskLoggerFactory.getLogger();
    private final byte[] nonce;

    public ServerPacketListenerImpl(SerializableHandler handler) {
        this.handler = handler;
        this.nonce = Ints.toByteArray(RandomGenerator.of("Xoroshiro128PlusPlus").nextInt());
    }

    @Override
    public void onHandShake(HandShakeC2SInfo packet) {
        handler.setId(packet.id());
        logger.info("HandShake received");
        KioskNettyServer server = (KioskNettyServer) handler.connection;
        this.handler.send(new HelloS2CPacket(server.getKeyPair().getPublic().getEncoded(), nonce));
        logger.info("Hello sent nonce : {}", nonce);
    }

    @Override
    public void onRequestData(UpdateDataPacket.RequestDataC2SPacket packet) {
        if (!this.handler.isEncrypted()) {
            throw new IllegalStateException("Client is not encrypted");
        }
        Registry<?> registry = RegistryManager.getAsId(packet.registryId());

        if (packet.registryId().equalsIgnoreCase("all")) {
            RegistryManager.entries().forEach(entry -> {
                this.handler.send(new UpdateDataPacket.ResponseDataS2CPacket(entry.getRegistryId(), (List<SynchronizeData<?>>) entry.getAll()));
            });

            return;
        }
        if (registry == null) {
            logger.warn("Registry {} not found", packet.registryId());
            logger.warn("skipping this packet...");
            return;
        }
        UpdateDataPacket.ResponseDataS2CPacket response = new UpdateDataPacket.ResponseDataS2CPacket(packet.registryId(), (List<SynchronizeData<?>>) registry.getAll());
        this.handler.send(response);
    }

    @Override
    public void onKeyReceived(KeyC2SPacket packet) {
        try {
            KioskNettyServer server = (KioskNettyServer) handler.connection;
            PrivateKey privateKey = server.getKeyPair().getPrivate();
            if (!packet.verifySignedNonce(this.nonce, privateKey)) {
                throw new IllegalStateException("Invalid nonce");
            }


            SecretKey secretKey = packet.decryptSecretKey(privateKey);
            Cipher encryptCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secretKey);
            Cipher decryptCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.DECRYPT_MODE, secretKey);

            this.handler.encrypt(encryptCipher, decryptCipher);
            this.handler.send(new EncryptCompleteS2CPacket(System.currentTimeMillis()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(DataAddedC2SPacket packet) {
        Registry<?> registry = RegistryManager.getAsId(packet.registryId());
        registry.add(packet.data().getRegistryElementId(), packet.data());

        KioskNettyServer server = (KioskNettyServer) handler.connection;
        server.getHandlers().forEach(handler ->
                handler.send(new UpdateDataPacket.ResponseDataS2CPacket(
                                registry.getRegistryId(),
                                (List<SynchronizeData<?>>) registry.getAll()
                        )
                )
        );
    }

    @Override
    public SidedPacket.Side getSide() {
        return SidedPacket.Side.SERVER;
    }

    @Override
    public SerializableHandler getHandler() {
        return this.handler;
    }
}
