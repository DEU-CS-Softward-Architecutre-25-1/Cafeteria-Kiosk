package dev.qf.server.database;

import common.network.SynchronizeData;
import common.registry.Registry;
import common.registry.RegistryManager;

public interface ExternalDataManager {
    default void loadAll() {
        RegistryManager.entries().forEach(this::loadSpecificRegistry);
    }
    default void saveAll() {
        RegistryManager.entries().forEach(this::saveSpecificRegistry);
    }
    default void close() {
        this.saveAll();
        internalClose();
    }
    void internalClose();
    void initialize();
    void loadSpecificRegistry(Registry<?> registry);
    void saveSpecificRegistry(Registry<?> registry);
    void removeSpecificRegistry(Registry<?> registry, String targetId);
    void saveSpecificRegistry(Registry<?> registry, SynchronizeData<?> data);
}
