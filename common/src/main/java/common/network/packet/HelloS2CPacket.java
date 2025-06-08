package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.encryption.NetworkEncryptionUtils;
import common.network.handler.listener.ClientPacketListener;
import common.util.JavaCodecs;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;

public final class HelloS2CPacket implements SidedPacket<ClientPacketListener> {
    public static final Codec<HelloS2CPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    JavaCodecs.BYTE_ARRAY.fieldOf("public_key").forGetter(HelloS2CPacket::getPublicKeyAsByte),
                    JavaCodecs.BYTE_ARRAY.fieldOf("nonce").forGetter(HelloS2CPacket::getNonceAsByte)
            ).apply(instance, HelloS2CPacket::new)
    );
    private final byte[] publicKey;
    private final byte[] nonce;
    private volatile Byte[] nonceAsByte;
    private volatile Byte[] publicKeyAsByte;

    public HelloS2CPacket(Byte[] publicKey, Byte[] nonce) {
        this.publicKey = JavaCodecs.asByteArray(publicKey);
        this.nonce = JavaCodecs.asByteArray(nonce);
        this.nonceAsByte = nonce;
        this.publicKeyAsByte = publicKey;
    }

    public HelloS2CPacket(byte[] publicKey, byte[] nonce) {
        this.publicKey = publicKey;
        this.nonce = nonce;
    }

    @Override
    public Side getSide() {
        return Side.CLIENT;
    }

    @Override
    public void apply(ClientPacketListener listener) {
        listener.onHello(this);
    }

    public PublicKey getPublicKey() {
        return NetworkEncryptionUtils.decodeEncodedRsaPublicKey(this.publicKey);
    }

    @Override
    public String getPacketId() {
        return "hello_s2c_packet";
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }

    @ApiStatus.Internal
    private Byte[] getNonceAsByte() {
        if (nonceAsByte == null) {
            this.nonceAsByte = JavaCodecs.asBoxingByteArray(nonce);
        }
        return nonceAsByte;
    }

    @ApiStatus.Internal
    private Byte[] getPublicKeyAsByte() {
        if (publicKeyAsByte == null) {
            this.publicKeyAsByte = JavaCodecs.asBoxingByteArray(publicKey);
        }
        return publicKeyAsByte;
    }

    public byte[] publicKey() {
        return publicKey;
    }

    public byte[] nonce() {
        return nonce;
    }
}
