package common.network;

import common.network.handler.SerializableDecoder;
import common.network.handler.SerializableEncoder;
import common.network.handler.SerializableHandler;
import common.network.packet.SidedPacket;
import io.netty.channel.*;
import org.jetbrains.annotations.ApiStatus;

public interface Connection {
    void run();
    void setChannel(Channel channel);
    Channel getChannel();
    void shutdown();
    @ApiStatus.Internal
    default ChannelInitializer<Channel> initializeChannelInitializer(SidedPacket.Side side) {
        return new ChannelInitializer<>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                try {
                    ch.setOption(ChannelOption.TCP_NODELAY, true);
                } catch (Exception ignored) {
                    // NOP
                }

                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("serializable_decoder", new SerializableDecoder());
                pipeline.addLast("encoder", new SerializableEncoder());
                pipeline.addLast("handler", new SerializableHandler(side));
            }
        };
    }
    ChannelFuture sendSerializable(Serializable<?> serializable);
}
