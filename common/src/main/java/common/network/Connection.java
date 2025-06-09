package common.network;

import common.network.handler.*;
import common.network.packet.Serializable;
import common.network.packet.SidedPacket;
import io.netty.channel.*;
import io.netty.handler.flow.FlowControlHandler;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * 각 사이드의 네트워크 구현체가 상속하는 인터페이스이다. 해당 인터페이스에는 실행, 셧다운, 연결된 핸들러 획득, 채널 파이프라인 설정 메소드가 정의되어 있다.
 */
@ApiStatus.NonExtendable
public interface Connection {
    ChannelFuture run();
    void shutdown();
    @ApiStatus.Internal
    default ChannelInitializer<Channel> initializeChannelInitializer(SidedPacket.Side side) {
        return new ChannelInitializer<>() {

            @Override
            protected void initChannel(Channel ch) {
                try {
                    ch.setOption(ChannelOption.TCP_NODELAY, true);
                    ch.setOption(ChannelOption.SO_KEEPALIVE, true);
                } catch (Exception ignored) {
                    // NOP
                }

                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("splitter", new SplitterHandler());
                pipeline.addLast(new FlowControlHandler());
                pipeline.addLast("decoder", new SerializableDecoder());
                pipeline.addLast("prepender", new SizePrepender());
                pipeline.addLast("encoder", new SerializableEncoder());
                pipeline.addLast("handler", new SerializableHandler(side));
            }
        };
    }
    SidedPacket.Side getSide();
    ChannelFuture sendSerializable(String id, Serializable<?> serializable);
    void handleDisconnect(ChannelHandlerContext ctx, SerializableHandler handler);
    void onEstablishedChannel(ChannelHandlerContext ctx, SerializableHandler handler);
    List<SerializableHandler> getHandlers();
}
