package dev.qf.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import common.network.packet.HandShakeC2SInfo;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import common.event.ChannelEstablishedEvent;
import dev.qf.client.network.ClientPacketListenerFactory;
import dev.qf.client.network.KioskNettyClient;
import common.network.handler.factory.PacketListenerFactory;
import common.util.Container;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final KioskNettyClient INSTANCE = new KioskNettyClient();
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static final ScheduledExecutorService REGISTRY_REFRESH_EXECUTOR = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("RegistryRefreshThread")
                    .setUncaughtExceptionHandler((t, e) -> KioskLoggerFactory.getLogger().error("Registry refresh thread error", e))
                    .build()
    );
    private static Thread mainThread;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        mainThread = Thread.currentThread();

        // 1. 이벤트 리스너를 먼저 등록하여 레이스 컨디션을 해결합니다.
        ChannelEstablishedEvent.EVENT.register((handler -> mainThread.interrupt()));

        // 2. 새로운 스레드에서 비동기적으로 서버 연결을 시작합니다.
        new Thread(INSTANCE::run).start();

        // 3. 안전하게 연결 완료 이벤트를 기다립니다.
        synchronized (mainThread) {
            try {
                mainThread.wait();
            } catch (InterruptedException ignored) {
                // 이벤트가 발생하면 이곳으로 와서 대기가 풀립니다.
            }
        }

        LOGGER.info("Channel Established. Requesting handshake...");
        INSTANCE.sendSerializable(new HandShakeC2SInfo("test"));

        // 4. 주기적인 동기화는 나중을 위해 예약하고,
        REGISTRY_REFRESH_EXECUTOR.scheduleAtFixedRate(INSTANCE::sendSyncPacket, 5,5, TimeUnit.MINUTES);

        // 5. 지금 당장 필요한 데이터를 요청합니다.
        LOGGER.info("Requesting initial data synchronization...");
        INSTANCE.sendSyncPacket();

        LOGGER.info("Waiting for CATEGORIES data to be populated...");
        while (RegistryManager.CATEGORIES.size() == 0) {
            Thread.sleep(1000);
        }

        LOGGER.info("Data populated. Creating GUI...");

        // 6. OwnerMainUI를 실행하기 위해 필요한 ClientOrderService를 생성합니다.
        ClientOrderService clientOrderService = new ClientOrderService();
        clientOrderService.setKioskClient(Main.INSTANCE);

        // 7. 최종적으로 OwnerMainUI를 생성하고 실행합니다.
        SwingUtilities.invokeAndWait(() -> {
            OwnerMainUI ui = new OwnerMainUI(clientOrderService);
            ui.setVisible(true);
        });
    }

    static {
        // 클라이언트 실행 시 ClientPacketListenerFactory를 사용하도록 설정합니다.
        Container.put(PacketListenerFactory.class, new ClientPacketListenerFactory());
    }
}