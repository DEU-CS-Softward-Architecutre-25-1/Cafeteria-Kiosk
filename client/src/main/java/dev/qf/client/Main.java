package dev.qf.client;

import common.network.packet.HandShakeC2SInfo;
import common.registry.RegistryManager;
import dev.qf.client.network.KioskNettyClient;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class Main {
    public static KioskNettyClient INSTANCE = new KioskNettyClient();
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        INSTANCE.run();
        while (!INSTANCE.isConnected()) {
            Thread.sleep(100);
        }

        INSTANCE.sendSerializable(new HandShakeC2SInfo("test"));

        while (RegistryManager.CATEGORIES.size() == 0) {
            Thread.sleep(1000);
        }
        SwingUtilities.invokeAndWait(() -> {new CategoryManagementUI().setVisible(true);});
    }
}
