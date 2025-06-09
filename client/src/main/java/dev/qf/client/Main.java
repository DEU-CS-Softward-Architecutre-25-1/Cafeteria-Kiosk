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

        LOGGER.info("Channel Established. Requesting handshake...");
        INSTANCE.sendSerializable(new HandShakeC2SInfo("test"));

        REGISTRY_REFRESH_EXECUTOR.scheduleAtFixedRate(INSTANCE::sendSyncPacket, 5,5, TimeUnit.MINUTES);

        LOGGER.info("Waiting for Order data to be populated...");
        while (RegistryManager.CATEGORIES.size() == 0) {
            Thread.sleep(1000);
        }

        LOGGER.info("Data populated. Creating GUI...");

        clientOrderService = new ClientOrderService();

        SwingUtilities.invokeAndWait(() -> {
            OwnerMainUI ui = new OwnerMainUI(clientOrderService);
            ui.setVisible(true);
        });
    }

    public static ClientOrderService getClientOrderService() {
        return clientOrderService;
    }

    static {
        Container.put(PacketListenerFactory.class, new ClientPacketListenerFactory());
    }
}