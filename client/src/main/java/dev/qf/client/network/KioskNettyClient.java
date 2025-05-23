package dev.qf.client.network;

import common.KioskLoggerFactory;
import common.network.Connection;
import common.network.Serializable;
import common.network.SerializableManager;
import common.network.packet.SidedPacket;
import common.util.Container;
import dev.qf.client.Main;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;

public class KioskNettyClient implements Connection {
    private static final int port = 8192;
    private final MultiThreadIoEventLoopGroup CHANNEL = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    private Bootstrap bootstrap;
    public static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private Channel channel;

    public KioskNettyClient() {
        if (Main.INSTANCE != null) {
            throw new IllegalStateException("KioskNettyClient already initialized");
        }
        Container.put(Connection.class, this);
    }

    @Override
    public void run() {

            bootstrap = new Bootstrap();
            bootstrap.group(CHANNEL);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            SerializableManager.initialize();

            bootstrap.handler(this.initializeChannelInitializer(SidedPacket.Side.CLIENT));

            this.setChannel(bootstrap.connect("localhost", port).syncUninterruptibly().channel());


    }

    public void shutdown() {
        LOGGER.info("Shutting down client...");
        if (channel != null && channel.isOpen()) {
            try {
                channel.close().syncUninterruptibly();
            } catch (Exception e) {
                LOGGER.warn("Exception while closing client channel", e);
            }
        }
        if (!CHANNEL.isShuttingDown() && !CHANNEL.isShutdown()) {
            CHANNEL.shutdownGracefully().syncUninterruptibly();
        }
        LOGGER.info("Client shutdown complete.");
    }

    @Override
    public ChannelFuture sendSerializable(Serializable<?> serializable) {
        return this.channel.writeAndFlush(serializable).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel getChannel() {
        return this.channel;
    }

    public ChannelFuture connect(String host, int port) {
        return bootstrap.connect(host, port).syncUninterruptibly();
    }

}
