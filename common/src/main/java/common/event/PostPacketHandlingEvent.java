package common.event;

import common.network.handler.SerializableHandler;
import common.network.packet.Serializable;

/**
 * 디버깅을 위한 이벤트. 이 프로젝트에서는 굳이 제네릭한 이벤트를 사용할 이유가 없음.
 */
@FunctionalInterface
public interface PostPacketHandlingEvent {
    Event<PostPacketHandlingEvent> EVENT = EventFactory.createArrayBacked(PostPacketHandlingEvent.class, listeners ->
            (handler, packet) -> {
                for (PostPacketHandlingEvent listener : listeners) {
                    listener.onPostPacketHandle(handler, packet);
                }
            });

    void onPostPacketHandle(SerializableHandler handler, Serializable<?> packet);
}
