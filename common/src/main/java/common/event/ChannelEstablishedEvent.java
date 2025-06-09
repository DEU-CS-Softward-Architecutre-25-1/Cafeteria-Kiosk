package common.event;

import common.network.handler.SerializableHandler;

@FunctionalInterface
public interface ChannelEstablishedEvent {
    Event<ChannelEstablishedEvent> EVENT = EventFactory.createArrayBacked(ChannelEstablishedEvent.class, listeners -> handler -> {
        for(var listener : listeners) {
            listener.onChannelEstablished(handler);
        }
    });
    void onChannelEstablished(SerializableHandler handler);
}
