package dev.qf.server.network;

import common.KioskLoggerFactory;
import common.network.Connection;
import common.network.Serializable;
import common.network.SerializableManager;
import common.network.packet.SidedPacket;
import common.util.Container;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

public class KioskNettyServer implements Connection {
    //TODO MOVE TO CONFIG
    private static final int port = 8192;
    private final MultiThreadIoEventLoopGroup CHANNEL = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final MultiThreadIoEventLoopGroup WORKER = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    public static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private Channel channel;

    public KioskNettyServer() {
        if (Container.get(Connection.class) != null) {
            throw new IllegalStateException("Server is already started");
        }
        Container.put(Connection.class, this);
    }

    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(CHANNEL, WORKER);
            serverBootstrap.channel(NioServerSocketChannel.class);
            SerializableManager.initialize();

            serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
            serverBootstrap.childHandler(this.initializeChannelInitializer(SidedPacket.Side.SERVER));
            serverBootstrap.bind(port).syncUninterruptibly().channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("Failed to start server.");
            LOGGER.error(e.getMessage());
        }
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
        if (!WORKER.isShuttingDown() && !WORKER.isShutdown()) {
            WORKER.shutdownGracefully().syncUninterruptibly();
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
}
