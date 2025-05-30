package dev.qf.client.event;

import common.event.Event;
import common.event.EventFactory;
import common.registry.Registry;

@FunctionalInterface
public interface DataReceivedEvent {
    Event<DataReceivedEvent> EVENT = EventFactory.createArrayBacked(DataReceivedEvent.class, (listeners) -> (data) -> {
        for (DataReceivedEvent listener : listeners) {
            listener.onRegistryChanged(data);
        }
    });

    void onRegistryChanged(Registry<?> registry);
}
