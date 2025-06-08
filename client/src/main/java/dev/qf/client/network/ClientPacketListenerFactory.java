package dev.qf.client.network;

import common.network.handler.listener.PacketListener;
import common.network.handler.SerializableHandler;
import common.network.handler.factory.PacketListenerFactory;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ClientPacketListenerFactory implements PacketListenerFactory {
    @Override
    public PacketListener getPacketListener(SerializableHandler channel) {
        return new ClientPacketListenerImpl(channel);
    }
}
