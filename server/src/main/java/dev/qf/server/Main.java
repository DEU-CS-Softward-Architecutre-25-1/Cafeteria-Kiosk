package dev.qf.server;

import common.KioskLoggerFactory;
import common.network.Connection;
import dev.qf.server.network.KioskNettyServer;

public class Main {
    public static Connection INSTANCE = new KioskNettyServer();
    public static void main(String[] args) {

        INSTANCE.run();
        KioskLoggerFactory.getLogger().info("Server started");
    }
}
