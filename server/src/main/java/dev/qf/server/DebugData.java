package dev.qf.server;

import common.Category;
import common.Menu;
import common.Option;
import common.OptionGroup;
import common.registry.RegistryManager;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.util.List;

/**
 * this must be executed test scenarios. this will ruins system when runtime.
 */
@TestOnly
public class DebugData {

    DebugData() {

    }

    public void generateDebugData() {
        System.out.println("=== Generating Debug Data ===");

        Option testOption1 = new Option("testOption1", "테스트", 1000);
        OptionGroup testOptionGroup = new OptionGroup("testOptionGroup1", "테스트옵션그룹", true, List.of(testOption1));

        // soldOut 필드를 포함한 메뉴 생성
        Menu testMenu1 = new Menu("testMenu1", "아메리카노", 4000, Path.of(""), "진한 에스프레소", List.of(testOptionGroup), false);
        Menu testMenu2 = new Menu("testMenu2", "카페라떼", 4500, Path.of(""), "부드러운 라떼", List.of(testOptionGroup), false);
        Menu testMenu3 = new Menu("testMenu3", "카푸치노", 4500, Path.of(""), "거품이 풍성한", List.of(testOptionGroup), true); // 품절
        Menu testMenu4 = new Menu("testMenu4", "에스프레소", 3500, Path.of(""), "진짜 진한", List.of(testOptionGroup), false);

        Menu dessertMenu1 = new Menu("dessertMenu1", "치즈케이크", 6000, Path.of(""), "달콤한 치즈케이크", List.of(testOptionGroup), false);
        Menu dessertMenu2 = new Menu("dessertMenu2", "티라미수", 6500, Path.of(""), "이탈리아 디저트", List.of(testOptionGroup), false);

        // 카테고리별로 다른 메뉴들 배치
        Category coffeeCategory = new Category("testCategory1", "커피", List.of(testMenu1, testMenu2, testMenu3, testMenu4));
        Category dessertCategory = new Category("testCategory2", "디저트", List.of(dessertMenu1, dessertMenu2));
        Category drinkCategory = new Category("testCategory3", "음료", List.of());
        Category bakeryCategory = new Category("testCategory4", "베이커리", List.of());
        Category saladCategory = new Category("testCategory5", "샐러드", List.of());

        // Registry에 추가 - 순서 중요!
        RegistryManager.OPTIONS.add(testOption1.name(), testOption1);
        RegistryManager.OPTION_GROUPS.add(testOptionGroup.name(), testOptionGroup);

        // 메뉴들 추가
        RegistryManager.MENUS.add(testMenu1.id(), testMenu1);
        RegistryManager.MENUS.add(testMenu2.id(), testMenu2);
        RegistryManager.MENUS.add(testMenu3.id(), testMenu3);
        RegistryManager.MENUS.add(testMenu4.id(), testMenu4);
        RegistryManager.MENUS.add(dessertMenu1.id(), dessertMenu1);
        RegistryManager.MENUS.add(dessertMenu2.id(), dessertMenu2);

        // 카테고리들 추가
        RegistryManager.CATEGORIES.add(coffeeCategory.cateId(), coffeeCategory);
        RegistryManager.CATEGORIES.add(dessertCategory.cateId(), dessertCategory);
        RegistryManager.CATEGORIES.add(drinkCategory.cateId(), drinkCategory);
        RegistryManager.CATEGORIES.add(bakeryCategory.cateId(), bakeryCategory);
        RegistryManager.CATEGORIES.add(saladCategory.cateId(), saladCategory);

        System.out.println("Debug data generation completed!");
        System.out.println("Categories: " + RegistryManager.CATEGORIES.size());
        System.out.println("Menus: " + RegistryManager.MENUS.size());
        System.out.println("Options: " + RegistryManager.OPTIONS.size());
        System.out.println("Option Groups: " + RegistryManager.OPTION_GROUPS.size());
    }
}