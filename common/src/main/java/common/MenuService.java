package common;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class MenuService {
    private final List<Menu> menuList = new ArrayList<>();
    private final List<Category> categoryList;
    private int nextId = 1;

    public MenuService(List<Category> categoryList) {
        this.categoryList = categoryList;
    }

    public boolean registerMenu(String name, double price, String categoryId, String description, String imagePath, boolean soldOut) {
        if (name == null || name.trim().isEmpty()) return false;
        if (price < 0) return false;
        if (categoryId == null || categoryId.trim().isEmpty()) return false;

        boolean categoryExists = categoryList.stream()
                .anyMatch(c -> c.cateId().equals(categoryId));
        if (!categoryExists) return false;

        Menu newMenu = new Menu(nextId++, name, price, categoryId, description, imagePath, soldOut);
        menuList.add(newMenu);
        return true;
    }

    public boolean deleteMenu(int menuId) {
        return menuList.removeIf(menu -> menu.menuId() == menuId);
    }

    public int deleteMenusByCategory(String categoryId) {
        int deletedCount = 0;
        List<Menu> menusToDelete = new ArrayList<>();

        for (Menu menu : menuList) {
            if (menu.categoryId().equals(categoryId)) {
                menusToDelete.add(menu);
            }
        }

        for (Menu menu : menusToDelete) {
            if (menuList.remove(menu)) {
                deletedCount++;
            }
        }

        return deletedCount;
    }

    public List<Menu> getAllMenus() {
        return new ArrayList<>(menuList);
    }

    public List<Menu> getMenusByCategory(String categoryId) {
        return menuList.stream()
                .filter(menu -> menu.categoryId().equals(categoryId))
                .toList();
    }

    public String getCategoryName(String categoryId) {
        return categoryList.stream()
                .filter(c -> c.cateId().equals(categoryId))
                .map(Category::cateName)
                .findFirst()
                .orElse("알 수 없는 카테고리");
    }

    public boolean toggleSoldOut(int menuId) {
        Optional<Menu> menuOpt = menuList.stream()
                .filter(menu -> menu.menuId() == menuId)
                .findFirst();

        if (menuOpt.isEmpty()) return false;

        Menu oldMenu = menuOpt.get();
        Menu updatedMenu = new Menu(
                oldMenu.menuId(),
                oldMenu.name(),
                oldMenu.price(),
                oldMenu.categoryId(),
                oldMenu.description(),
                oldMenu.imagePath(),
                !oldMenu.soldOut()
        );

        menuList.remove(oldMenu);
        menuList.add(updatedMenu);
        return true;
    }

    public Optional<Menu> getMenuById(int menuId) {
        return menuList.stream()
                .filter(menu -> menu.menuId() == menuId)
                .findFirst();
    }

}
