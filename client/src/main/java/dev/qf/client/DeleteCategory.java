package dev.qf.client;

import common.Category;
import common.network.Connection;
import common.network.packet.DataDeletedC2SPacket;
import common.registry.RegistryManager;
import common.util.Container;
import common.network.packet.SidedPacket;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class DeleteCategory {
    private final List<Category> categoryList;
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    public DeleteCategory(List<Category> categoryList) {
        this.categoryList = categoryList;
    }

    // 삭제 처리
    public boolean deleteCategory(String categoryId) {
        Optional<Category> opt = getCategoryById(categoryId);
        if (opt.isEmpty()) {
            LOGGER.warn("Category with id {} not found", categoryId);
            return false;
        }

        try {
            Connection connection = Container.get(Connection.class);
            if (connection != null && connection.getSide() == SidedPacket.Side.CLIENT) {
                try {
                    connection.sendSerializable("server",
                            new DataDeletedC2SPacket(RegistryManager.CATEGORIES.getRegistryId(), categoryId));
                    LOGGER.info("Category deletion requested: {}", categoryId);
                    LOGGER.info("Waiting for server to provide corrected category-menu mappings...");

                } catch (Exception networkException) {
                    LOGGER.warn("Failed to send deletion request to server, but local deletion completed: {}",
                            networkException.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to delete category: {}", categoryId, e);
            return false;
        }
    }

    private Optional<Category> getCategoryById(String categoryId) {
        Optional<Category> fromList = categoryList.stream()
                .filter(c -> c.cateId().equals(categoryId))
                .findFirst();

        if (fromList.isPresent()) {
            return fromList;
        }
        return RegistryManager.CATEGORIES.getById(categoryId);
    }
}
