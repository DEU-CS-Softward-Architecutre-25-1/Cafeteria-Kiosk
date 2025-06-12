package dev.qf.client.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import common.network.packet.HandShakeC2SInfo;
import common.network.packet.UpdateDataPacket;
import common.util.KioskLoggerFactory;
import common.network.Connection;
import common.network.packet.Serializable;
import common.network.SerializableManager;
import common.network.handler.SerializableHandler;
import common.network.handler.factory.PacketListenerFactory;
import common.network.packet.SidedPacket;
import common.util.Container;
import dev.qf.client.config.Config;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public final class KioskNettyClient implements Connection {
    public static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static final int port = 8192;
    private static final ScheduledExecutorService RECONNECT_SCHEDULER;
    private static final ScheduledExecutorService PACKET_QUEUE_SCHEDULER;
    private static final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final MultiThreadIoEventLoopGroup CHANNEL = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private Bootstrap bootstrap;
    private volatile SerializableHandler handler;
    private final ReentrantLock lock = new ReentrantLock();
    private PriorityQueue<Serializable<?>> pendingQueue = new ObjectArrayFIFOQueue<>();

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
        registerExecutorService();
        Config config = Config.getInstance();
        isConnecting.set(true);
        LOGGER.info("Connecting to server...");
        return bootstrap.connect(config.getHost(), config.getPort()).syncUninterruptibly().addListener((future) -> {
            isConnecting.set(false);
        });
    }

    private void registerExecutorService() {
        RECONNECT_SCHEDULER.scheduleAtFixedRate(() -> {
            if (this.handler == null || !this.handler.channel.isOpen()) {
                if (isConnecting.get()) {
                    LOGGER.info("Other Protocol trying to connect. wait for 5 seconds...");
                }
                Config config = Config.getInstance();
                LOGGER.info("Reconnecting to server...");
                try {
                    ChannelFuture future = this.bootstrap.connect(config.getHost(), config.getPort()).syncUninterruptibly();
                    future.addListener((future1) -> {
                        if (future1.isSuccess()) {
                            LOGGER.info("Reconnected to server.");
                            LOGGER.info("Sending HandShakeC2SInfo...");
                            this.sendSerializable(new HandShakeC2SInfo(config.getKioskId()));
                        } else {
                            LOGGER.error("Reconnect failed. retry after 5 seconds...", future1.cause());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Reconnect failed. retry after 5 seconds...", e.getCause());
                }
            }
        }, 5, 5, TimeUnit.SECONDS);


        PACKET_QUEUE_SCHEDULER.scheduleAtFixedRate(() -> {
            if (handler == null || !handler.isOpened()) {
                return;
            }
            try {
                this.lock.lock();
                if (pendingQueue.isEmpty()) {
                    return;
                }
                while(pendingQueue.isEmpty()) {
                    this.sendSerializable(pendingQueue.dequeue());
                }
            } finally {
                this.lock.unlock();
            }
        }, 5, 1, TimeUnit.SECONDS);
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
        LOGGER.warn("Connection is lost. : {}", ctx.channel().remoteAddress());
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
        if (this.handler == null) {
            if (isConnecting.get()) {
                LOGGER.warn("Client is Connecting... Add packet into Queue");

            } else {
                isConnecting.set(true);
                bootstrap.connect(Config.getInstance().getHost(), port)
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                        .addListener((ChannelFutureListener) future -> {
                            isConnecting.set(false);
                            if (future.isSuccess()) {
                                LOGGER.info("Connected to server.");
                                LOGGER.info("Sending HandShakeC2SInfo...");
                                this.sendSerializable(new HandShakeC2SInfo(Config.getInstance().getKioskId()));
                            }
                        });
            }
            this.addPendingPacket(serializable);
            return null;
        }
        return this.handler.send(serializable);
    }

    public ChannelFuture connect(String host, int port) {
        return bootstrap.connect(host, port).syncUninterruptibly();
    }

    public void sendSyncPacket() {
        LOGGER.info("Requesting All Synchronization items");
        this.sendSerializable(new UpdateDataPacket.RequestDataC2SPacket("all"));
    }

    private void addPendingPacket(Serializable<?> packet) {
        try {
            this.lock.lock();
            pendingQueue.enqueue(packet);
        }finally {
            this.lock.unlock();
        }
    }

    static {
        Container.put(PacketListenerFactory.class, new ClientPacketListenerFactory());
        RECONNECT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Reconnect thread error", e))
                .setNameFormat("ReconnectThread")
                .build()
        );

        PACKET_QUEUE_SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Packet Queue thread error", e))
                .setNameFormat("PacketQueueThread")
                .build()
        );
    }
}