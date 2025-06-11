// dev.qf.server.DebugData.java

package dev.qf.server;

import common.*;
import common.registry.Registry;
import common.registry.RegistryManager;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 이 클래스는 테스트 시나리오 실행에만 사용되어야 합니다. 런타임에 시스템을 망가뜨릴 수 있습니다.
 */
@TestOnly
public class DebugData {

    public DebugData() {
        // 생성자
    }

    public void generateDebugData() {
        try {
            RegistryManager.entries().forEach(Registry::unfreeze);
            System.out.println("=== 실제 메뉴 데이터 생성 중 (카테고리 정리 버전) ===");

            // --- 1. 공통 옵션 정의 ---
            Option hotOption = new Option("opt_hot", "뜨겁게", 0);
            Option coldOption = new Option("opt_cold", "차갑게", 500);
            Option shotAdd1 = new Option("opt_shot_1", "1샷 추가", 500);
            Option shotAdd2 = new Option("opt_shot_2", "2샷 추가", 1000);

            Option syrupMore = new Option("opt_syrup_more", "시럽 많이", 0);
            Option syrupNormal = new Option("opt_syrup_normal", "시럽 보통", 0);
            Option syrupLess = new Option("opt_syrup_less", "시럽 적게", 0);

            Option whippedCream = new Option("opt_whipped_cream", "휘핑크림", 500);
            Option sizeUp = new Option("opt_size_up", "사이즈업", 1000);

            // 컵 선택 옵션 변경
            Option tumblerOption = new Option("opt_tumbler", "텀블러", 0);
            Option disposableCupOption = new Option("opt_disposable_cup", "일회용품", 0);

            Option packing = new Option("opt_packing", "포장", 0);


            // --- 2. 공통 옵션 그룹 정의 ---
            OptionGroup tempGroup = new OptionGroup("group_temp", "온도 선택", true, List.of(hotOption, coldOption));
            OptionGroup shotGroup = new OptionGroup("group_shot", "샷 추가", false, List.of(shotAdd1, shotAdd2));

            // 바닐라 라떼 전용 시럽 그룹
            OptionGroup vanillaSyrupGroup = new OptionGroup("group_vanilla_syrup", "바닐라 시럽 양", false, List.of(syrupMore, syrupNormal, syrupLess));

            OptionGroup whippedCreamGroup = new OptionGroup("group_whipped_cream", "휘핑크림", false, List.of(whippedCream));
            OptionGroup sizeUpGroup = new OptionGroup("group_size_up", "사이즈", false, List.of(sizeUp));

            // 컵 선택 옵션 그룹 변경
            OptionGroup cupGroup = new OptionGroup("group_cup", "컵 선택", true, List.of(tumblerOption, disposableCupOption));

            OptionGroup packingGroup = new OptionGroup("group_packing", "포장 여부", false, List.of(packing));


            // --- 3. Registry에 옵션 및 옵션 그룹 추가 (이전 데이터 클리어 후 추가) ---
            RegistryManager.OPTIONS.clear(); // 기존 옵션 모두 제거
            RegistryManager.OPTIONS.add(hotOption.id(), hotOption);
            RegistryManager.OPTIONS.add(coldOption.id(), coldOption);
            RegistryManager.OPTIONS.add(shotAdd1.id(), shotAdd1);
            RegistryManager.OPTIONS.add(shotAdd2.id(), shotAdd2);
            RegistryManager.OPTIONS.add(syrupMore.id(), syrupMore);
            RegistryManager.OPTIONS.add(syrupNormal.id(), syrupNormal);
            RegistryManager.OPTIONS.add(syrupLess.id(), syrupLess);
            RegistryManager.OPTIONS.add(whippedCream.id(), whippedCream);
            RegistryManager.OPTIONS.add(sizeUp.id(), sizeUp);
            RegistryManager.OPTIONS.add(tumblerOption.id(), tumblerOption);
            RegistryManager.OPTIONS.add(disposableCupOption.id(), disposableCupOption);
            RegistryManager.OPTIONS.add(packing.id(), packing);

            RegistryManager.OPTION_GROUPS.clear(); // 기존 옵션 그룹 모두 제거
            RegistryManager.OPTION_GROUPS.add(tempGroup.id(), tempGroup);
            RegistryManager.OPTION_GROUPS.add(shotGroup.id(), shotGroup);
            RegistryManager.OPTION_GROUPS.add(vanillaSyrupGroup.id(), vanillaSyrupGroup);
            RegistryManager.OPTION_GROUPS.add(whippedCreamGroup.id(), whippedCreamGroup);
            RegistryManager.OPTION_GROUPS.add(sizeUpGroup.id(), sizeUpGroup);
            RegistryManager.OPTION_GROUPS.add(cupGroup.id(), cupGroup);
            RegistryManager.OPTION_GROUPS.add(packingGroup.id(), packingGroup);


            // --- 4. 메뉴 정의 ---
            Path defaultImagePath = Path.of("images/default_menu.png");

            // 커피 메뉴
            Menu espresso = new Menu("menu_espresso", "에스프레소", 3500, Path.of("images/coffee_espresso.png"), "강렬한 에스프레소", List.of(cupGroup), false);
            Menu americano = new Menu("menu_americano", "아메리카노", 4000, Path.of("images/coffee_americano.png"), "고소한 원두의 풍미", List.of(tempGroup, shotGroup, cupGroup), false);
            Menu latte = new Menu("menu_latte", "카페 라떼", 4500, Path.of("images/coffee_latte.png"), "부드러운 우유의 조화", List.of(tempGroup, shotGroup, cupGroup), false);
            Menu cappuccino = new Menu("menu_cappuccino", "카푸치노", 4500, Path.of("images/coffee_cappuccino.png"), "풍성한 거품의 부드러움", List.of(tempGroup, shotGroup, cupGroup), false);
            // 바닐라 라떼에 바닐라 시럽 양 옵션 그룹 추가
            Menu vanillaLatte = new Menu("menu_vanilla_latte", "바닐라 라떼", 5000, Path.of("images/coffee_vanilla_latte.png"), "달콤한 바닐라 향 라떼", List.of(tempGroup, shotGroup, vanillaSyrupGroup, cupGroup), false);
            Menu caramelMacchiato = new Menu("menu_caramel_macchiato", "카라멜 마키아또", 5500, Path.of("images/coffee_caramel_macchiato.png"), "달콤한 카라멜과 우유의 조화", List.of(tempGroup, shotGroup, whippedCreamGroup, cupGroup), false);

            // 음료 메뉴
            Menu greenTeaLatte = new Menu("menu_green_latte", "녹차 라떼", 5000, Path.of("images/beverage_greentea.png"), "제주 녹차의 깊은 맛", List.of(tempGroup, cupGroup), false);
            Menu chocoLatte = new Menu("menu_choco_latte", "초코 라떼", 4800, Path.of("images/beverage_choco.png"), "달콤쌉쌀한 초코 라떼", List.of(tempGroup, whippedCreamGroup, cupGroup), false);
            Menu grapefruitAde = new Menu("menu_grapefruit_ade", "자몽 에이드", 5500, Path.of("images/beverage_grapefruit.png"), "상큼한 자몽 에이드", List.of(cupGroup), false);
            Menu lemonAde = new Menu("menu_lemon_ade", "레몬 에이드", 5000, Path.of("images/beverage_lemon.png"), "톡 쏘는 레몬 에이드", List.of(cupGroup), false);
            Menu peachIcedTea = new Menu("menu_peach_iced_tea", "복숭아 아이스티", 4000, Path.of("images/beverage_peach_iced_tea.png"), "달콤한 복숭아 향", List.of(cupGroup), false);

            // 디저트 메뉴
            Menu cheesecake = new Menu("menu_cheesecake", "치즈케이크", 6000, Path.of("images/dessert_cheesecake.png"), "부드러운 뉴욕 치즈케이크", List.of(packingGroup), false);
            Menu tiramisu = new Menu("menu_tiramisu", "티라미수", 6500, Path.of("images/dessert_tiramisu.png"), "클래식 티라미수", List.of(packingGroup), false);
            Menu croffle = new Menu("menu_croffle", "크로플", 5000, Path.of("images/dessert_croffle.png"), "겉바속촉 크로플", List.of(whippedCreamGroup, packingGroup), false);
            Menu macaronSet = new Menu("menu_macaron_set", "마카롱 (3개)", 7500, Path.of("images/dessert_macaron.png"), "다양한 맛의 마카롱 세트", List.of(packingGroup), false);


            // --- 5. Registry에 메뉴 추가 (이전 데이터 클리어 후 추가) ---
            RegistryManager.MENUS.clear(); // 기존 메뉴 모두 제거
            // 커피 메뉴 추가
            RegistryManager.MENUS.add(espresso.id(), espresso);
            RegistryManager.MENUS.add(americano.id(), americano);
            RegistryManager.MENUS.add(latte.id(), latte);
            RegistryManager.MENUS.add(cappuccino.id(), cappuccino);
            RegistryManager.MENUS.add(vanillaLatte.id(), vanillaLatte);
            RegistryManager.MENUS.add(caramelMacchiato.id(), caramelMacchiato);

            // 음료 메뉴 추가
            RegistryManager.MENUS.add(greenTeaLatte.id(), greenTeaLatte);
            RegistryManager.MENUS.add(chocoLatte.id(), chocoLatte);
            RegistryManager.MENUS.add(grapefruitAde.id(), grapefruitAde);
            RegistryManager.MENUS.add(lemonAde.id(), lemonAde);
            RegistryManager.MENUS.add(peachIcedTea.id(), peachIcedTea);

            // 디저트 메뉴 추가
            RegistryManager.MENUS.add(cheesecake.id(), cheesecake);
            RegistryManager.MENUS.add(tiramisu.id(), tiramisu);
            RegistryManager.MENUS.add(croffle.id(), croffle);
            RegistryManager.MENUS.add(macaronSet.id(), macaronSet);


            Category coffeeCategory = new Category("cate_coffee", "커피", List.of(
                    espresso, americano, latte, cappuccino, vanillaLatte, caramelMacchiato
            ));
            Category beverageCategory = new Category("cate_beverage", "음료", List.of(
                    greenTeaLatte, chocoLatte, grapefruitAde, lemonAde, peachIcedTea
            ));
            Category dessertCategory = new Category("cate_dessert", "디저트", List.of(
                    cheesecake, tiramisu, croffle, macaronSet
            ));

            // --- 7. Registry에 카테고리 추가 (이전 데이터 클리어 후 추가) ---
            RegistryManager.CATEGORIES.clear(); // 기존 카테고리 모두 제거
            RegistryManager.CATEGORIES.add(coffeeCategory.cateId(), coffeeCategory);
            RegistryManager.CATEGORIES.add(beverageCategory.cateId(), beverageCategory);
            RegistryManager.CATEGORIES.add(dessertCategory.cateId(), dessertCategory);

            // --- 8. 테스트 주문 데이터 ---
            // (이전에 있었던 중복된 RegistryManager.add() 호출은 모두 제거되었습니다.)
            Cart cart1 = new Cart();
            cart1.addItem(new OrderItem(americano, Map.of(tempGroup, hotOption), 2));
            Order order1 = new Order(101, "김민준", LocalDateTime.now().minusMinutes(15), OrderStatus.ACCEPTED, cart1);

            Cart cart2 = new Cart();
            cart2.addItem(new OrderItem(latte, Map.of(tempGroup, coldOption, shotGroup, shotAdd1), 1));
            Order order2 = new Order(102, "이서아", LocalDateTime.now().minusMinutes(7), OrderStatus.PENDING, cart2);

            Cart cart3 = new Cart();
            cart3.addItem(new OrderItem(espresso, Map.of(cupGroup, disposableCupOption), 1));
            cart3.addItem(new OrderItem(macaronSet, Map.of(packingGroup, packing), 1));
            Order order3 = new Order(103, "박서준", LocalDateTime.now().minusMinutes(2), OrderStatus.PENDING, cart3);

            RegistryManager.ORDERS.add(String.valueOf(order1.orderId()), order1);
            RegistryManager.ORDERS.add(String.valueOf(order2.orderId()), order2);
            RegistryManager.ORDERS.add(String.valueOf(order3.orderId()), order3);


            System.out.println("실제 메뉴 데이터 생성이 완료되었습니다!");
            System.out.println("카테고리 수: " + RegistryManager.CATEGORIES.size());
            System.out.println("메뉴 수: " + RegistryManager.MENUS.size());
            System.out.println("옵션 수: " + RegistryManager.OPTIONS.size());
            System.out.println("옵션 그룹 수: " + RegistryManager.OPTION_GROUPS.size());
        } finally {
            RegistryManager.entries().forEach(Registry::freeze);
        }
    }
}