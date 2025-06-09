package common.network.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

public class PacketDecryptor extends MessageToMessageDecoder<ByteBuf> {
    private final PacketEncryptionManager encryptionManager;

    public PacketDecryptor(Cipher cipher) {
        this.encryptionManager = new PacketEncryptionManager(cipher);
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(this.encryptionManager.decrypt(ctx, msg));
    }
}
