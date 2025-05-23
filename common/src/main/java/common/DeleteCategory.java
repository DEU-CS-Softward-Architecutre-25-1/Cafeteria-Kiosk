package common;

import java.util.List;
import java.util.Optional;

public class DeleteCategory {
    private final List<Category> categoryList;

    public DeleteCategory(List<Category> categoryList) {
        this.categoryList = categoryList;
    }

    // 삭제 처리
    public boolean deleteCategory(String categoryId) {
        Optional<Category> opt = getCategoryById(categoryId);
        if (opt.isEmpty()) {
            return false;
        }
        Category toRemove = opt.get();
        categoryList.remove(toRemove);
        // 서버에 삭제 요청
        return true;
    }

    private Optional<Category> getCategoryById(String categoryId) {
        return categoryList.stream()
                .filter(c -> c.cateId().equals(categoryId))
                .findFirst();
    }
}