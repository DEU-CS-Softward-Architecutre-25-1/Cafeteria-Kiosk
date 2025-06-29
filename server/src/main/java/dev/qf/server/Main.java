package dev.qf.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import common.util.KioskLoggerFactory;
import common.network.handler.factory.PacketListenerFactory;
import common.util.Container;
import dev.qf.server.database.ExternalDataManager;
import dev.qf.server.database.LocalJsonStorage;
import dev.qf.server.database.CommonDBManager;
import dev.qf.server.network.KioskNettyServer;
import dev.qf.server.network.ServerPacketListenerFactory;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    public static final KioskNettyServer INSTANCE = new KioskNettyServer();
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static ExternalDataManager manager;
    private static final ScheduledExecutorService DB_BACKUP_SCHEDULER = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("external_data_backup_thread")
                    .setUncaughtExceptionHandler((t, e) -> KioskLoggerFactory.getLogger().error("DB backup thread error", e))
                    .build()
    );
    public static void main(String[] args) {
        OptionParser optionParser = new OptionParser();
        OptionSpec<Void> optionSpec = optionParser.accepts("debuggingItems");
        OptionSpec<String> storageType = optionParser.accepts("storageType").withRequiredArg().ofType(String.class);

        OptionSet optionSet = optionParser.parse(args);
        boolean debug = optionSet.has(optionSpec);
        if(debug) {
            LOGGER.info("Debugging items enabled");
            DebugData data = new DebugData();
            data.generateDebugData();
            manager = new CommonDBManager();
        } else {
            // 데이터베이스에 대한 부분을 분리해둔 이유는 testItem argument 가 존재하는데도 불구하고 데이터베이스를 연결하는 상황을 방지하기 위해서이다.
            if (optionSet.has(storageType)) {
                String value = storageType.value(optionSet);
                 manager = switch (value.toLowerCase()) {
                    case "json" -> new LocalJsonStorage();
                    case "sqlite" -> new CommonDBManager();
                    default -> throw new IllegalArgumentException("Invalid storage type, only accepts json or sqlite");
                };
            } else {
                LOGGER.warn("No storage type specified. Using default storage type");
                manager = new CommonDBManager();
            }
        }
        manager.initialize();
        if (debug) {
            manager.saveAll();
//            manager.loadAll();
        } else {
            manager.loadAll();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(manager::close));

        KioskLoggerFactory.getLogger().info("Server started");
        Container.put(PacketListenerFactory.class, new ServerPacketListenerFactory());

        DB_BACKUP_SCHEDULER.scheduleAtFixedRate(() -> manager.saveAll(), 5, 5, java.util.concurrent.TimeUnit.MINUTES);

        INSTANCE.run();
    }

    public static ExternalDataManager getManager() {
        if (manager == null) {
            throw new IllegalStateException("No external data manager found");
        }
        return manager;
    }

}
