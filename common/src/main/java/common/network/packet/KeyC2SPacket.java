package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.encryption.NetworkEncryptionUtils;
import common.network.handler.listener.ServerPacketListener;
import common.util.JavaCodecs;
import common.util.KioskLoggerFactory;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

public class KeyC2SPacket implements SidedPacket<ServerPacketListener> {
    public static final Codec<KeyC2SPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    JavaCodecs.BYTE_ARRAY.fieldOf("encryptedSecretKey").forGetter(KeyC2SPacket::getEncryptedSecretKeyAsByte),
                    JavaCodecs.BYTE_ARRAY.fieldOf("nonce").forGetter(KeyC2SPacket::getNonceAsByte)
            ).apply(instance, KeyC2SPacket::new)
    );

    private final byte[] encryptedSecretKey;
    private final byte[] nonce;
    private Byte[] nonceAsByte;
    private Byte[] encryptedSecretKeyAsByte;

    private Byte[] getNonceAsByte() {
        if (nonceAsByte == null) {
            this.nonceAsByte = JavaCodecs.asBoxingByteArray(nonce);
        }
        return nonceAsByte;
    }

    private Byte[] getEncryptedSecretKeyAsByte() {
        if (encryptedSecretKeyAsByte == null) {
            this.encryptedSecretKeyAsByte = JavaCodecs.asBoxingByteArray(encryptedSecretKey);
        }
        return encryptedSecretKeyAsByte;
    }


    public KeyC2SPacket(SecretKey secretKey, PublicKey publicKey, byte[] nonce) {
        this.nonce = NetworkEncryptionUtils.encrypt(publicKey, nonce);
        this.encryptedSecretKey = NetworkEncryptionUtils.encrypt(publicKey, secretKey.getEncoded());
        KioskLoggerFactory.getLogger().info("Encrypted nonce: {}", this.nonce);
    }

    public KeyC2SPacket(Byte[] encryptedSecretKeyAsByte, Byte[] nonceAsByte) {
        this.nonceAsByte = nonceAsByte;
        this.encryptedSecretKeyAsByte = encryptedSecretKeyAsByte;
        this.nonce = JavaCodecs.asByteArray(nonceAsByte);
        this.encryptedSecretKey = JavaCodecs.asByteArray(encryptedSecretKeyAsByte);
    }

    @Override
    public Side getSide() {
        return Side.SERVER;
    }

    @Override
    public void apply(ServerPacketListener listener) {
        listener.onKeyReceived(this);
    }

    @Override
    public String getPacketId() {
        return "key_c2s_packet";
    }

    public boolean verifySignedNonce(byte[] nonce, PrivateKey privateKey) {
        try {
            KioskLoggerFactory.getLogger().info("Verifying nonce... : {}", NetworkEncryptionUtils.decrypt(privateKey, this.nonce));
            return Arrays.equals(nonce, NetworkEncryptionUtils.decrypt(privateKey, this.nonce));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SecretKey decryptSecretKey(PrivateKey privateKey) {
        return NetworkEncryptionUtils.decryptSecretKey(privateKey, this.encryptedSecretKey);
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }
}
