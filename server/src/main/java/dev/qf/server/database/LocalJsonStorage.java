package dev.qf.server.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import common.network.SynchronizeData;
import common.registry.Registry;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class LocalJsonStorage implements ExternalDataManager {
    private final Logger logger = KioskLoggerFactory.getLogger();
    public static final Path LOCAL_STORAGE_ROOT = Path.of("data");
    private final Gson GSON = new Gson();

    @Override
    public void loadAll() {
        RegistryManager.entries().forEach(this::loadSpecificRegistry);
    }

    @Override
    public void saveAll() {
        RegistryManager.entries().forEach(this::saveSpecificRegistry);
    }

    @Override
    public void internalClose() {
        // NOP FileSystem always close when all IO Operation completes.
    }

    @Override
    public void initialize() {
        logger.info("Initializing local json storage");
        if (!Files.exists(LOCAL_STORAGE_ROOT)) {
            try {
                Files.createDirectories(LOCAL_STORAGE_ROOT);
            } catch (IOException e) {
                logger.error("Failed to create directory for local json storage");
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    public void loadSpecificRegistry(@NotNull Registry<?> registry) {
        try {
            registry.unfreeze();
            registry.clear();
            Path path = this.asPath(registry);
            if (createDirectoryIfNotExist(path)) {
                return;
            }
            try (var pathStream = Files.walk(path)) {
                pathStream.forEach(file -> {
                    if (Files.isRegularFile(file)) {
                        try {
                            String str = Files.readString(file);
                            DataResult<?> result = registry
                                    .getCodec()
                                    .decode(JsonOps.INSTANCE, GSON.fromJson(str, JsonObject.class));

                            SynchronizeData<?> serializable = (SynchronizeData<?>) result.getOrThrow();
                            registry.add(serializable.getRegistryElementId(), serializable);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to read directory for registry {}", registry.getRegistryId());
                logger.error(e.getMessage());
            }
        } finally {
            registry.freeze();
        }
    }

    @Override
    public void saveSpecificRegistry(Registry<?> registry) {
        Path path = this.asPath(registry);
        createDirectoryIfNotExist(path);
        registry.getAll().forEach(data -> {
            try {
                Files.writeString(path.resolve(data.getRegistryElementId() + ".json"), data.toJson().toString());
            } catch (IOException e) {
                logger.error("Failed to write data to file for registry {}", registry.getRegistryId());
                logger.error("file data : {}", data);
            }
        });
    }

    private Path asPath(Registry<?> registry) {
        return LOCAL_STORAGE_ROOT.resolve(registry.getRegistryId());
    }

    /**
     * create root directory if not exists
     * @param path registry path
     * @return true if created. else false.
     */
    private boolean createDirectoryIfNotExist(Path path) {
        if (!Files.exists(path)) {
            logger.info("Creating directory for registry {}", path);
            try {
                Files.createDirectories(path);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}
