package common;

import common.network.Connection;
import common.network.packet.DataAddedC2SPacket;
import common.network.packet.DataDeletedC2SPacket;
import common.network.packet.SidedPacket;
import common.registry.RegistryManager;
import common.util.Container;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MenuService {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    private static final AtomicInteger nextId = new AtomicInteger(1);

    private static MenuService instance;

    private MenuService() {}

    public static synchronized MenuService getInstance() {
        if (instance == null) {
            instance = new MenuService();
        }
        return instance;
    }

    public boolean registerMenu(String name, int price, String categoryId, String description, String imagePath, boolean soldOut) {
        if (name == null || name.trim().isEmpty()) return false;
        if (price < 0) return false;
        if (categoryId == null || categoryId.trim().isEmpty()) return false;
        boolean nameExists = RegistryManager.MENUS.getAll().stream()
                .anyMatch(menu -> menu.name().equals(name.trim()));
        if (nameExists) {
            LOGGER.warn("Menu with name '{}' already exists", name);
            return false;
        }
        try {
            String menuId = "menu_" + nextId.getAndIncrement();
            Path imagePathObj = imagePath != null && !imagePath.trim().isEmpty() ?
                    Path.of(imagePath) : Path.of("");

            Menu newMenu = new Menu(menuId, name, price, imagePathObj, description, List.of(), soldOut);


            Optional<Category> categoryOpt = RegistryManager.CATEGORIES.getById(categoryId);
            Category updatedCategory = null;
            if (categoryOpt.isPresent()) {
                Category oldCategory = categoryOpt.get();
                List<Menu> updatedMenuList = new ArrayList<>(oldCategory.menus());
                updatedMenuList.add(newMenu);

                updatedCategory = new Category(oldCategory.cateId(), oldCategory.cateName(), updatedMenuList);
            }


            Connection connection = Container.get(Connection.class);
            if (connection != null && connection.getSide() == SidedPacket.Side.CLIENT) {
                try {
                    // 메뉴 전송
                    connection.sendSerializable("server",
                            new DataAddedC2SPacket(RegistryManager.MENUS.getRegistryId(), newMenu));
                    LOGGER.info("Menu registration requested: {} for category: {}", name, categoryId);

                    // 업데이트된 카테고리 전송
                    if (updatedCategory != null) {
                        connection.sendSerializable("server",
                                new DataAddedC2SPacket(RegistryManager.CATEGORIES.getRegistryId(), updatedCategory));
                        LOGGER.info("Updated category sent to server: {} with {} menus",
                                updatedCategory.cateName(), updatedCategory.menus().size());

                        // 카테고리의 메뉴 리스트 로깅
                        for (Menu menu : updatedCategory.menus()) {
                            LOGGER.debug("  Category '{}' contains menu: {} ({})",
                                    updatedCategory.cateName(), menu.name(), menu.id());
                        }
                    } else {
                        LOGGER.warn("Could not find updated category '{}' to send to server", categoryId);
                    }

                } catch (Exception networkException) {
                    LOGGER.warn("Failed to send registration request, but local registration completed: {}",
                            networkException.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to register menu: {}", name, e);
            return false;
        }
    }

    public boolean deleteMenu(String menuId) {
        Optional<Menu> menuOpt = RegistryManager.MENUS.getById(menuId);
        if (menuOpt.isEmpty()) {
            LOGGER.warn("Menu with id {} not found", menuId);
            return false;
        }

        try {
            Connection connection = Container.get(Connection.class);
            if (connection != null && connection.getSide() == SidedPacket.Side.CLIENT) {
                try {
                    connection.sendSerializable("server",
                            new DataDeletedC2SPacket(RegistryManager.MENUS.getRegistryId(), menuId));
                    LOGGER.info("Menu deletion requested: {}", menuId);
                } catch (Exception networkException) {
                    LOGGER.warn("Failed to send deletion request to server, but local deletion completed: {}",
                            networkException.getMessage());
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to delete menu: {}", menuId, e);
            return false;
        }
    }

    public List<Menu> getMenusByCategory(String categoryId) {
        Optional<Category> categoryOpt = RegistryManager.CATEGORIES.getById(categoryId);
        if (categoryOpt.isPresent()) {
            return new ArrayList<>(categoryOpt.get().menus());
        }
        return List.of();
    }

    public boolean toggleSoldOut(String menuId) {
        Optional<Menu> menuOpt = RegistryManager.MENUS.getById(menuId);
        if (menuOpt.isEmpty()) {
            LOGGER.warn("Menu with id {} not found", menuId);
            return false;
        }

        try {
            Menu oldMenu = menuOpt.get();
            boolean newSoldOutStatus = !oldMenu.soldOut();
            Menu updatedMenu = oldMenu.withSoldOut(newSoldOutStatus);

            LOGGER.info("Toggling soldOut status: {} - {} -> {}",
                    oldMenu.name(), oldMenu.soldOut(), newSoldOutStatus);

            // 카테고리 메뉴 업데이트
            List<Category> updatedCategories = updateMenuInAllCategories(oldMenu, updatedMenu);
            LOGGER.debug("Menu updated in all categories");

            //서버에 업데이트 요청
            Connection connection = Container.get(Connection.class);
            if (connection != null && connection.getSide() == SidedPacket.Side.CLIENT) {
                try {
                    connection.sendSerializable("server",
                            new DataAddedC2SPacket(RegistryManager.MENUS.getRegistryId(), updatedMenu));
                    LOGGER.info("Menu soldOut toggle request sent to server: {} -> {}",
                            oldMenu.name(), newSoldOutStatus);

                    for (Category category : updatedCategories) {
                        connection.sendSerializable("server",
                                new DataAddedC2SPacket(RegistryManager.CATEGORIES.getRegistryId(), category));
                        LOGGER.debug("Updated category '{}' sent to server after menu toggle", category.cateName());
                    }

                } catch (Exception networkException) {
                    LOGGER.warn("Failed to send toggle request to server, but local toggle completed: {}",
                            networkException.getMessage());
                }
            } else {
                LOGGER.warn("No connection available to send toggle request to server");
            }

            Optional<Menu> verifyMenu = RegistryManager.MENUS.getById(menuId);
            if (verifyMenu.isPresent()) {
                LOGGER.info("Toggle verification: {} soldOut status is now {}",
                        verifyMenu.get().name(), verifyMenu.get().soldOut());
            }

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to toggle soldOut status for menu: {}", menuId, e);
            return false;
        }
    }

    // TODO : find side effect
    private List<Category> updateMenuInAllCategories(Menu oldMenu, Menu newMenu) {
        LOGGER.debug("Updating menu {} in all categories", newMenu.name());

        List<Category> updatedCategories = new ArrayList<>();
        for (Category category : RegistryManager.CATEGORIES.getAll()) {
            List<Menu> menuList = new ArrayList<>(category.menus());
            boolean updated = false;

            for (int i = 0; i < menuList.size(); i++) {
                Menu currentMenu = menuList.get(i);
                if (currentMenu.id().equals(oldMenu.id())) {
                    menuList.set(i, newMenu);
                    updated = true;
                    LOGGER.debug("Menu {} updated at position {} in category {}",
                            newMenu.name(), i, category.cateName());
                    break;
                }
            }

            if (updated) {
                Category updatedCategory = new Category(
                        category.cateId(),
                        category.cateName(),
                        menuList
                );

                updatedCategories.add(updatedCategory);
                LOGGER.debug("Category {} updated with new menu data", category.cateName());
            }
        }

        LOGGER.info("Menu {} updated in {} categories", newMenu.name(), updatedCategories.size());
        return updatedCategories;
    }
}
