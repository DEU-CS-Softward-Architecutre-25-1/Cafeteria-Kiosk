package dev.qf.server.database;

import common.network.SynchronizeData;
import common.network.packet.Serializable;
import common.registry.Registry;
import common.util.KioskLoggerFactory;
import dev.qf.server.database.io.KioskDBSerializer;
import me.mrnavastar.sqlib.SQLib;
import me.mrnavastar.sqlib.api.DataStore;
import me.mrnavastar.sqlib.api.database.Database;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.impl.config.NonMinecraft;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

public class SQLiteStorage implements ExternalDataManager {
    private final Logger logger = KioskLoggerFactory.getLogger();
    private Database database;

    @Override
    public void internalClose() {
        this.database.close();
    }

    @Override
    public void initialize() {
        NonMinecraft.init(Path.of("sqlite"), Path.of("sqlite", "config"));
        database = SQLib.getDatabase();
    }

    @Override
    public void loadSpecificRegistry(@NotNull Registry<?> registry) {
        try {
            registry.unfreeze();
            registry.clear();
            DataStore store = database.dataStore("kiosk", registry.getRegistryId());
            store.getContainers().forEach(container -> {
                String elementId = container.get(JavaTypes.STRING, "id").orElseGet(()->"unknown");
                KioskLoggerFactory.getLogger().info("Loading {}", elementId);
                try {
                    SynchronizeData<?> synchronizeData = KioskDBSerializer.deserialize((Class<SynchronizeData<?>>) registry.getClazz(), container);
                    registry.add(synchronizeData.getRegistryElementId(), synchronizeData);
                } catch (Exception e) {
                    logger.error("Failed to deserialize data for registry element {}", registry.getRegistryId());
                    logger.error(e.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to load registry {}", registry.getRegistryId());
            logger.error(e.getMessage());
        } finally {
            registry.freeze();
        }
    }

    @Override
    public void saveSpecificRegistry(Registry<?> registry) {
        DataStore store = database.dataStore("kiosk", registry.getRegistryId());
        registry.getAll().forEach(data -> {
            KioskLoggerFactory.getLogger().info("Saving {}", data.getRegistryElementId());
            try {
                KioskDBSerializer.serialize(store.getOrCreateContainer("id", data.getRegistryElementId()), (SynchronizeData<?>) data);
            } catch (Exception e) {
                logger.error("Failed to serialize data for registry element {}", data.getRegistryElementId());
                logger.error("data : {}", data);
                logger.error(e.getMessage());
            }
        });
    }
}
