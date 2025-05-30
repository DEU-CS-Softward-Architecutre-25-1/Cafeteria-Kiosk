package common.network.handler;

import common.network.encoding.VariableInts;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class SizePrepender extends MessageToByteEncoder<ByteBuf> {
    public static final int MAX_PREPEND_LENGTH = 3;

    public SizePrepender() {
    }

    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2) {
        int i = byteBuf.readableBytes();
        int j = VariableInts.getSizeInBytes(i);
        if (j > 3) {
            throw new EncoderException("Packet too large: size " + i + " is over 8");
        } else {
            byteBuf2.ensureWritable(j + i);
            VariableInts.write(byteBuf2, i);
            byteBuf2.writeBytes(byteBuf, byteBuf.readerIndex(), i);
        }
    }
}