package common.network.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

public class PacketEncryptionManager {
    private final Cipher cipher;
    private byte[] conversionBuffer = new byte[0];
    private byte[] encryptionBuffer = new byte[0];

    PacketEncryptionManager(Cipher cipher) {
        this.cipher = cipher;
    }

    private byte[] toByteArray(ByteBuf buf) {
        int i = buf.readableBytes();
        if (i > conversionBuffer.length) {
            conversionBuffer = new byte[i];
        }
        buf.readBytes(conversionBuffer, 0, i);
        return conversionBuffer;
    }

    protected ByteBuf decrypt(ChannelHandlerContext ctx, ByteBuf buf) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        ByteBuf byteBuf = ctx.alloc().heapBuffer(this.cipher.getOutputSize(i));
        int bytesWritten = this.cipher.update(bs, 0, i, byteBuf.array(), byteBuf.arrayOffset());
        if (bytesWritten < 0 || bytesWritten > byteBuf.capacity()) {
            throw new IllegalStateException("Invalid number of bytes written by cipher.update: " + bytesWritten);
        }
        byteBuf.writerIndex(bytesWritten);
        return byteBuf;
    }

    protected void encrypt(ByteBuf buf, ByteBuf result) throws ShortBufferException {
        int byteSize = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        int outputSize = this.cipher.getOutputSize(byteSize);

        if (this.encryptionBuffer.length < outputSize) {
            this.encryptionBuffer = new byte[outputSize];
        }

        result.writeBytes(this.encryptionBuffer, 0, this.cipher.update(bs, 0, byteSize, this.encryptionBuffer));
    }
}
