package common.network.handler;

import common.util.KioskLoggerFactory;
import common.network.packet.Serializable;
import common.network.encoding.StringEncodings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * {@link Serializable}를 {@link com.google.gson.JsonObject}로, 그걸 다시 {@link ByteBuf}로 변환한다. <br>
 * @see StringEncodings#encode(ByteBuf, CharSequence, int)
 * @see Serializable#toJson()
 */
@ApiStatus.Internal
public class SerializableEncoder extends MessageToByteEncoder<Serializable<?>> {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable<?> msg, ByteBuf out) {
            StringEncodings.encode(out, msg.toJson().toString(), 32767);
            int i = out.readableBytes();
            LOGGER.info("OUT : [{}] -> {} bytes", msg.getPacketId(), i);
    }
}
