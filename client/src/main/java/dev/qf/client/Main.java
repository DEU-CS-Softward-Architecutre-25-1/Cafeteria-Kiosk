package dev.qf.client;

import common.network.Connection;
import dev.qf.client.network.KioskNettyClient;

public class Main {
    public static Connection INSTANCE = new KioskNettyClient();
    public static void main(String[] args) {

    }
}
