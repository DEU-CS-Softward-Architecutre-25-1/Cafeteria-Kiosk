package dev.qf.client.network;

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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PublicKey;

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

//        this.handler.send(); // TODO IMPLEMENT SEND PUBLIC KEY

        KeyC2SPacket secretPacket = new KeyC2SPacket(secretKey, publicKey, packet.nonce());
        logger.info("Client public key sent");
        this.handler.send(secretPacket);
        this.handler.encrypt(encrpytionCipher, decryptionCipher);
   }

    @Override
    public void onReceivedData(UpdateDataPacket.ResponseDataS2CPacket packet) {
        logger.info("Received data : {}", packet.registryId());
        logger.info("data info : {}", packet);
        Registry<? extends SynchronizeData<?>> registry =  RegistryManager.getAsId(packet.registryId());
        if (registry == null) {
            logger.error("Received data from unknown registry : {}", packet.registryId());
        }

        registry.addAll(packet.data());
        DataReceivedEvent.EVENT.invoker().onRegistryChanged(registry);
    }

    @Override
    public void onEncryptCompleted(EncryptCompleteS2CPacket packet) {
        handler.send(new UpdateDataPacket.RequestDataC2SPacket("all"));
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
