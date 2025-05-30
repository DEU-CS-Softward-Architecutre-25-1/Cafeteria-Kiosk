package common;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class CategoryService {
    private static CategoryService instance;
    private final List<Category> categoryList = new CopyOnWriteArrayList<>();
    private final List<CategoryChangeListener> listeners = new ArrayList<>();

    private CategoryService() {
    }

    public static synchronized CategoryService getInstance() {
        if (instance == null) {
            instance = new CategoryService();
        }
        return instance;
    }

    public interface CategoryChangeListener {
        void onCategoriesChanged();
    }

    public void addChangeListener(CategoryChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (CategoryChangeListener listener : listeners) {
            listener.onCategoriesChanged();
        }
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categoryList);
    }

    public boolean addCategory(String categoryId, String categoryName) {
        if (categoryId == null || categoryId.isBlank()) return false;
        if (categoryName == null || categoryName.isBlank()) return false;

        boolean exists = categoryList.stream()
                .anyMatch(c -> c.cateId().equals(categoryId) || c.cateName().equalsIgnoreCase(categoryName));

        if (exists) return false;

        Category newCategory = new Category(categoryId, categoryName, new ArrayList<>());
        categoryList.add(newCategory);
        notifyListeners();
        return true;
    }

    public boolean deleteCategory(String categoryId) {
        boolean removed = categoryList.removeIf(c -> c.cateId().equals(categoryId));
        if (removed) {
            notifyListeners();
        }
        return removed;
    }

    public List<Category> getCategoryList() {
        return categoryList;
    }
}
