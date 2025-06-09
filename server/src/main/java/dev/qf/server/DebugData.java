package dev.qf.server;

import common.*;
import common.registry.RegistryManager;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        Option tempHot = new Option("temp_hot", "뜨겁게", 0);
        Option tempCold = new Option("temp_cold", "차갑게", 500);
        Option shot1 = new Option("shot_1", "1샷 추가", 500);
        Option syrupVanilla = new Option("syrup_vanilla", "바닐라 시럽", 300);
        OptionGroup tempGroup = new OptionGroup("group_temp", "온도", true, List.of(tempHot, tempCold));
        OptionGroup shotGroup = new OptionGroup("group_shot", "샷", false, List.of(shot1));
        OptionGroup syrupGroup = new OptionGroup("group_syrup", "시럽", false, List.of(syrupVanilla));

        Menu americano = new Menu("menu_americano", "아메리카노", 4000, Path.of("images/menu1.png"), "고소한 원두의 풍미", List.of(tempGroup, shotGroup));
        Menu latte = new Menu("menu_latte", "카페 라떼", 4500, Path.of("images/menu2.png"), "부드러운 우유의 조화", List.of(tempGroup, shotGroup, syrupGroup));

        Menu dessertMenu1 = new Menu("dessertMenu1", "치즈케이크", 6000, Path.of("images/menu3.png"), "달콤한 치즈케이크", List.of(testOptionGroup), false);
        Menu dessertMenu2 = new Menu("dessertMenu2", "티라미수", 6500, Path.of("images/menu4.png"), "이탈리아 디저트", List.of(testOptionGroup), false);
        Category coffeeCategory = new Category("cate_coffee", "커피", List.of(americano, latte));

        // 카테고리별로 다른 메뉴들 배치
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
        Cart cart1 = new Cart();
        cart1.addItem(new OrderItem(americano, Map.of(tempGroup, tempHot), 2));
        Order order1 = new Order(101, "김민준", LocalDateTime.now().minusMinutes(15), OrderStatus.ACCEPTED, cart1);

        Cart cart2 = new Cart();
        cart2.addItem(new OrderItem(latte, Map.of(tempGroup, tempCold, shotGroup, shot1), 1));
        Order order2 = new Order(102, "이서아", LocalDateTime.now().minusMinutes(7), OrderStatus.PENDING, cart2);

        Cart cart3 = new Cart();
        cart3.addItem(new OrderItem(americano, Map.of(tempGroup, tempCold), 1));
        cart3.addItem(new OrderItem(latte, Map.of(tempGroup, tempHot, syrupGroup, syrupVanilla), 1));
        Order order3 = new Order(103, "박서준", LocalDateTime.now().minusMinutes(2), OrderStatus.PENDING, cart3);

        RegistryManager.OPTIONS.add(tempHot.id(), tempHot);
        RegistryManager.OPTIONS.add(tempCold.id(), tempCold);
        RegistryManager.OPTIONS.add(shot1.id(), shot1);
        RegistryManager.OPTIONS.add(syrupVanilla.id(), syrupVanilla);

        RegistryManager.OPTION_GROUPS.add(tempGroup.id(), tempGroup);
        RegistryManager.OPTION_GROUPS.add(shotGroup.id(), shotGroup);
        RegistryManager.OPTION_GROUPS.add(syrupGroup.id(), syrupGroup);

        RegistryManager.MENUS.add(americano.id(), americano);
        RegistryManager.MENUS.add(latte.id(), latte);

        RegistryManager.CATEGORIES.add(coffeeCategory.cateId(), coffeeCategory);

        RegistryManager.ORDERS.add(String.valueOf(order1.orderId()), order1);
        RegistryManager.ORDERS.add(String.valueOf(order2.orderId()), order2);
        RegistryManager.ORDERS.add(String.valueOf(order3.orderId()), order3);
    }
}
