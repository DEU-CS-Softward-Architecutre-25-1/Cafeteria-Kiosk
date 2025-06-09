package dev.qf.client;

import common.Category;
import common.network.Connection;
import common.network.packet.DataAddedC2SPacket;
import common.registry.RegistryManager;
import common.util.Container;
import dev.qf.client.network.KioskNettyClient;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class AddCategory {
    private final List<Category> categoryList;
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$"); // 카테고리 ID는 영문과 숫자만 허용
    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$"); // 카테고리 이름은 한글, 영문, 숫자, 공백만 허용

    public AddCategory(List<Category> categoryList) {
        this.categoryList = categoryList;
    }

    // 입력값과 중복 확인
    private boolean contentsVerification(String categoryId, String categoryName) {
        if (categoryId == null || categoryId.isBlank()) return false;
        if (categoryName == null || categoryName.isBlank()) return false;

        // 입력값 확인
        if (!ID_PATTERN.matcher(categoryId).matches()) return false;
        if (!NAME_PATTERN.matcher(categoryName).matches()) return false;

        // 중복확인
        return categoryList.stream().noneMatch(c -> c.cateId().equals(categoryId) || c.cateName().equalsIgnoreCase(categoryName)
        );
    }

    public boolean addCategory(String categoryId, String categoryName) {
        if (!contentsVerification(categoryId, categoryName)) {
            return false;
        }

        // 네트워크 연결 확인
        Connection connection = Container.get(Connection.class);
        if (connection == null) {
            Category newCategory = new Category(categoryId, categoryName, new ArrayList<>());
            categoryList.add(newCategory);
            RegistryManager.CATEGORIES.add(categoryId, newCategory);
            return true;
        }

        // 네트워크 연결이 있는 경우 서버에 전송
        Category newCategory = new Category(categoryId, categoryName, new ArrayList<>());
        KioskNettyClient client = (KioskNettyClient) connection;
        try {
            client.sendSerializable(new DataAddedC2SPacket(RegistryManager.CATEGORIES.getRegistryId(), newCategory));
            return true;
        } catch (Exception e) {
            categoryList.add(newCategory);
            RegistryManager.CATEGORIES.add(categoryId, newCategory);
            return true;
        }
    }
}
