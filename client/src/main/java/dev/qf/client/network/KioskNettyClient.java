package dev.qf.client.network;

import common.network.packet.UpdateDataPacket;
import common.util.KioskLoggerFactory;
import common.network.Connection;
import common.network.packet.Serializable;
import common.network.SerializableManager;
import common.network.handler.SerializableHandler;
import common.network.handler.factory.PacketListenerFactory;
import common.network.packet.SidedPacket;
import common.util.Container;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;

import java.util.List;

public final class KioskNettyClient implements Connection {
    private static final int port = 8192;
    private final MultiThreadIoEventLoopGroup CHANNEL = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    private Bootstrap bootstrap;
    public static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private SerializableHandler handler;

    public KioskNettyClient() {
        if (Container.get(Connection.class) != null) {
            throw new IllegalStateException("KioskNettyClient already initialized");
        }
        Container.put(Connection.class, this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    public ChannelFuture run() {

            bootstrap = new Bootstrap();
            bootstrap.group(CHANNEL);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            SerializableManager.initialize();

            bootstrap.handler(this.initializeChannelInitializer(SidedPacket.Side.CLIENT));

            return bootstrap.connect("localhost", port).syncUninterruptibly();
    }

    public void shutdown() {
        LOGGER.info("Shutting down client...");
        if (handler != null && handler.channel.isOpen()) {
            try {
                handler.channel.close().syncUninterruptibly();
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
    public SidedPacket.Side getSide() {
        return SidedPacket.Side.CLIENT;
    }

    @Override
    public ChannelFuture sendSerializable(String id, Serializable<?> serializable) {

        /*
        클라이언트에서 특정 상황(예: 네트워크 연결 직후, 연결이 일시적으로 끊겼을 때)에서 데이터를 전송하려고 시도할 때
        원인 불명의 NullPointerException이 발생하여 비정상적으로 종료되는 문제가 있었습니다.
        
        SerializableHandler의 send() 메소드는 네트워크 연결이 불안정할 경우, ChannelFuture 객체 대신 null을 반환하는데
        기존에는 send() 메소드의 반환값이 null일 가능성을 전혀 확인하지 않고, 곧바로 .addListener() 메소드를 호출하여서
        null에 대고 메소드를 호출하려고 시도했기 때문에 NullPointerException이 발생한 것 같아
        handler.send()의 결과를 임시 변수(future)에 먼저 저장한 뒤
        그 변수가 null이 아닌 경우에만 .addListener()를 호출하도록 코드를 임시 수정.
         */

        ChannelFuture future = handler.send(serializable);
        if (future != null) {
            future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
        return future;
    }

    public boolean isConnected() {
        return handler != null && handler.channel != null && handler.channel.isOpen();
    }

    @Override
    public void handleDisconnect(ChannelHandlerContext ctx, SerializableHandler handler) {
        this.handler = null;
    }

    @Override
    public void onEstablishedChannel(ChannelHandlerContext ctx, SerializableHandler handler) {
        LOGGER.info("Connection is established. : {}", ctx.channel().remoteAddress());
        this.handler = handler;
    }

    @Override
    public List<SerializableHandler> getHandlers() {
        return handler != null ? List.of(handler) : List.of();
    }

    public ChannelFuture sendSerializable(Serializable<?> serializable) {
        return this.handler.send(serializable);
    }

    public ChannelFuture connect(String host, int port) {
        return bootstrap.connect(host, port).syncUninterruptibly();
    }

    public void sendSyncPacket() {
        LOGGER.info("Requesting All Synchronization items");
        this.sendSerializable(new UpdateDataPacket.RequestDataC2SPacket("all"));
    }

    static {
        Container.put(PacketListenerFactory.class, new ClientPacketListenerFactory());
    }
}