package dev.qf.server.database;

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
}
