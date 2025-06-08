package common.network.handler.factory;

import common.network.handler.listener.PacketListener;
import common.network.handler.SerializableHandler;
import org.jetbrains.annotations.ApiStatus;

/**
 * {@link PacketListenerFactory}는 {@link PacketListener}를 생성하는 팩토리 구현체로, 각 사이드 마다 알맞은 Listener 를 생성해
 * {@link SerializableHandler} 에게 제공한다.
 */
@ApiStatus.NonExtendable
public interface PacketListenerFactory {
    PacketListener getPacketListener(SerializableHandler handler);
}
