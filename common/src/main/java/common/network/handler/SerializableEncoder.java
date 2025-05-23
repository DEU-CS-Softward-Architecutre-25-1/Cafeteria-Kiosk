package common.network.handler;

import common.KioskLoggerFactory;
import common.network.Serializable;
import common.network.encoding.StringEncodings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;

public class SerializableEncoder extends MessageToByteEncoder<Serializable<?>> {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable<?> msg, ByteBuf out) throws Exception {
            StringEncodings.encode(out, msg.toJson().toString(), 65535);
            int i = out.readableBytes();
            LOGGER.info("OUT : [{}] -> {} bytes", msg.getPacketId(), i);
    }
}
