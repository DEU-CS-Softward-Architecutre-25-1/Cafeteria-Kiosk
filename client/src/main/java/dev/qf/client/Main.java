package dev.qf.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import common.OrderService;
import common.event.ChannelEstablishedEvent;
import common.network.packet.HandShakeC2SInfo;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import common.event.ChannelEstablishedEvent;
import dev.qf.client.event.LoginEvent;
import dev.qf.client.network.ClientPacketListenerFactory;
import dev.qf.client.network.KioskNettyClient;
import common.network.handler.factory.PacketListenerFactory;
import common.util.Container;
import org.slf4j.Logger;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final KioskNettyClient INSTANCE = new KioskNettyClient();
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static ClientOrderService clientOrderService;
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
        ChannelEstablishedEvent.EVENT.register((handler -> mainThread.interrupt()));

        INSTANCE.run();

        synchronized (mainThread) {
            try {
                mainThread.wait();
            } catch (InterruptedException ignored) {
            }
        }

        if (!INSTANCE.isConnected()) {
            LOGGER.error("서버에 연결할 수 없습니다.");
            JOptionPane.showMessageDialog(null,
                    "서버를 먼저 실행해주세요.",
                    "연결 오류",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        LOGGER.info("서버에 연결됨. HandShake 전송...");
        INSTANCE.sendSerializable(new HandShakeC2SInfo("test"));

        clientOrderService = new ClientOrderService();

        REGISTRY_REFRESH_EXECUTOR.scheduleAtFixedRate(INSTANCE::sendSyncPacket, 5,5, TimeUnit.MINUTES);

        // Registry 데이터 대기 (타임아웃 추가)
        LOGGER.info("서버 데이터 대기 중...");
        int dataWaitAttempts = 0;
        while (RegistryManager.CATEGORIES.size() == 0 && dataWaitAttempts < 50) {
            Thread.sleep(200);
            dataWaitAttempts++;
            if (dataWaitAttempts % 10 == 0) {
                System.out.println("데이터 대기 중... (" + dataWaitAttempts + "/50)");
            }
        }

        if (RegistryManager.CATEGORIES.size() == 0) {
            LOGGER.error("서버로부터 데이터를 받지 못했습니다.");
            JOptionPane.showMessageDialog(null,
                    "서버로부터 데이터를 받지 못했습니다.",
                    "데이터 오류",
                    JOptionPane.ERROR_MESSAGE);
            System.out.println("데이터 없이 UI 실행...");
        } else {
            LOGGER.info("데이터 로드 완료. 카테고리 수: {}", RegistryManager.CATEGORIES.size());
        }

        SwingUtilities.invokeAndWait(() -> {
            LoginUI login = new LoginUI();
            login.setVisible(true);
        });
    }

    public static ClientOrderService getClientOrderService() {
        return clientOrderService;
    }

    static {
        Container.put(PacketListenerFactory.class, new ClientPacketListenerFactory());
    }
}