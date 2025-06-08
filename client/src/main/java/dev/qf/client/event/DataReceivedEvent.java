package dev.qf.client.event;

import common.event.Event;
import common.event.EventFactory;
import common.network.handler.SerializableHandler;
import common.registry.Registry;

@FunctionalInterface
public interface DataReceivedEvent {
    Event<DataReceivedEvent> EVENT = EventFactory.createArrayBacked(DataReceivedEvent.class, (listeners) ->
    (handler,data) -> {
        for (DataReceivedEvent listener : listeners) {
            listener.onRegistryChanged(handler, data);
        }
    });

    void onRegistryChanged(SerializableHandler handler, Registry<?> registry);
}
