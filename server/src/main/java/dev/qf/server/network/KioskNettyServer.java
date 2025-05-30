package dev.qf.server.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import common.util.KioskLoggerFactory;
import common.network.Connection;
import common.network.Serializable;
import common.network.SerializableManager;
import common.network.handler.SerializableHandler;
import common.network.packet.SidedPacket;
import common.util.Container;
import dev.qf.server.network.encrypt.ServerNetworkEncryptionUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

import java.security.KeyPair;
import java.util.*;

public final class KioskNettyServer implements Connection {
    //TODO MOVE TO CONFIG
    private static final int port = 8192;
    private final MultiThreadIoEventLoopGroup BOSS = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final MultiThreadIoEventLoopGroup WORKER = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private KeyPair keyPair;
    public static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private final List<SerializableHandler> connections = Collections.synchronizedList(Lists.newArrayList());

    public KioskNettyServer() {
        if (Container.get(Connection.class) != null) {
            throw new IllegalStateException("Server is already started");
        }
        Container.put(Connection.class, this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    private void generateKeyPair() {
        LOGGER.info("Generating key pair...");
         keyPair = ServerNetworkEncryptionUtils.generateKeyPair();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(BOSS, WORKER);
            serverBootstrap.channel(NioServerSocketChannel.class);
            SerializableManager.initialize();

            serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));
            serverBootstrap.childHandler(this.initializeChannelInitializer(SidedPacket.Side.SERVER));

            this.generateKeyPair();

            serverBootstrap.bind(port).syncUninterruptibly().channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("Failed to start server.");
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down client...");
        this.connections.forEach(handler -> {
            Channel channel = handler.channel;
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close().syncUninterruptibly();
                } catch (Exception e) {
                    LOGGER.warn("Exception while closing client channel", e);
                }
            }
        });

        if (!BOSS.isShuttingDown() && !BOSS.isShutdown()) {
            BOSS.shutdownGracefully().syncUninterruptibly();
        }
        if (!WORKER.isShuttingDown() && !WORKER.isShutdown()) {
            WORKER.shutdownGracefully().syncUninterruptibly();
        }
        LOGGER.info("Client shutdown complete.");
    }

    @Override
    public SidedPacket.Side getSide() {
        return SidedPacket.Side.SERVER;
    }

    @Override
    public ChannelFuture sendSerializable(String id, Serializable<?> serializable) {
        Optional<SerializableHandler> optionalHandler =  connections.stream().filter(handler -> id.equals(handler.getId())).findAny();

        SerializableHandler handler =  optionalHandler.orElseThrow(() -> new IllegalArgumentException("No handler found for id " + id));

        return handler.send(serializable);
    }

    @Override
    public void handleDisconnect(ChannelHandlerContext ctx, SerializableHandler handler) {
        LOGGER.info("Connection closed : {}", ctx.channel().remoteAddress());
        this.connections.remove(handler);
    }

    @Override
    public void onEstablishedChannel(ChannelHandlerContext ctx, SerializableHandler handler) {
        LOGGER.info("New connection established : {}", ctx.channel().remoteAddress());
        this.connections.add(handler);
    }

    @Override
    public List<SerializableHandler> getHandlers() {
        return ImmutableList.copyOf(this.connections);
    }
}
