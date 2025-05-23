package common.network.handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import common.KioskLoggerFactory;
import common.network.Serializable;
import common.network.SerializableManager;
import common.network.encoding.StringEncodings;
import common.util.JsonHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class SerializableDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    public static Gson gson = new Gson();
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int i = in.readableBytes();
        if (i != 0) {
            String msg;
            try {
                msg = StringEncodings.decode(in, 65535);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return;
            }
            JsonObject jsonObject = gson.fromJson(msg, JsonObject.class);
            String type = JsonHelper.getString(jsonObject, Serializable.PACKET_ID_PROPERTY);

            if (in.readableBytes() > 0) {
                throw new IOException(
                        "Packet " +
                        type +
                        " was larger than expected; found " +
                        in.readableBytes() +
                        " extra bytes while reading the packet."
                );
            } else {
                Codec<?> codec = SerializableManager.getCodec(type).orElseThrow(() -> new IllegalArgumentException("Unknown packet type: " + type));
                out.add(codec.decode(JsonOps.INSTANCE, jsonObject.get(Serializable.DATA_PROPERTY)).getOrThrow().getFirst());


                LOGGER.info(SerializableManager.SERIALIZABLE_RECEIVED_MARKER, "IN [{}] -> {} bytes", type, i);
            }
        }
    }
}
