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
        Option tempHot = new Option("temp_hot", "뜨겁게", 0);
        Option tempCold = new Option("temp_cold", "차갑게", 500);
        Option shot1 = new Option("shot_1", "1샷 추가", 500);
        Option syrupVanilla = new Option("syrup_vanilla", "바닐라 시럽", 300);
        OptionGroup tempGroup = new OptionGroup("group_temp", "온도", true, List.of(tempHot, tempCold));
        OptionGroup shotGroup = new OptionGroup("group_shot", "샷", false, List.of(shot1));
        OptionGroup syrupGroup = new OptionGroup("group_syrup", "시럽", false, List.of(syrupVanilla));

        Menu americano = new Menu("menu_americano", "아메리카노", 4000, Path.of("images/americano.jpg"), "고소한 원두의 풍미", List.of(tempGroup, shotGroup));
        Menu latte = new Menu("menu_latte", "카페 라떼", 4500, Path.of("images/latte.jpg"), "부드러운 우유의 조화", List.of(tempGroup, shotGroup, syrupGroup));

        Category coffeeCategory = new Category("cate_coffee", "커피", List.of(americano, latte));

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