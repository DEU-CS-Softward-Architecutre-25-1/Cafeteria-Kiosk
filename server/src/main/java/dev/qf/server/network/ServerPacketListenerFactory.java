package dev.qf.server.network;

import common.network.handler.listener.PacketListener;
import common.network.handler.SerializableHandler;
import common.network.handler.factory.PacketListenerFactory;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ServerPacketListenerFactory implements PacketListenerFactory {
    @Override
    public PacketListener getPacketListener(SerializableHandler handler) {
        return new ServerPacketListenerImpl(handler);
    }
}
