package common.network.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;

public class PacketEncryptor extends MessageToByteEncoder<ByteBuf> {
    private final PacketEncryptionManager encryptionManager;

    public PacketEncryptor(Cipher cipher) {
        this.encryptionManager = new PacketEncryptionManager(cipher);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
        this.encryptionManager.encrypt(msg, out);
    }
}
