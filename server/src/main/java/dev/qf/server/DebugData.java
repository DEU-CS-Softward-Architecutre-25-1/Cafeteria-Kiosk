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
        Option optRegular = new Option("opt_regular", "Regular", 0);
        Option optLarge = new Option("opt_large", "Large", 500);
        Option optAddShot = new Option("opt_add_shot", "샷 추가", 500);
        Option optVanilla = new Option("opt_vanilla", "바닐라 시럽", 300);
        Option optHazelnut = new Option("opt_hazelnut", "헤이즐넛 시럽", 300);

        OptionGroup ogSize = new OptionGroup("og_size", "사이즈", true, List.of(optRegular, optLarge));
        OptionGroup ogShot = new OptionGroup("og_shot", "샷", false, List.of(optAddShot));
        OptionGroup ogSyrup = new OptionGroup("og_syrup", "시럽", false, List.of(optVanilla, optHazelnut));

        Menu menuAmericano = new Menu("menu_americano", "아메리카노", 4000, Path.of(""), "고소한 원두의 풍미", List.of(ogSize, ogShot));
        Menu menuLatte = new Menu("menu_latte", "카페 라떼", 4500, Path.of(""), "부드러운 우유와 에스프레소의 조화", List.of(ogSize, ogShot, ogSyrup));

        Category cateCoffee = new Category("cate_coffee", "커피", List.of(menuAmericano, menuLatte));

        Cart cart1 = new Cart();
        cart1.addItem(new OrderItem(menuAmericano, Map.of(ogSize, optLarge, ogShot, optAddShot), 1));
        Order order1 = new Order(101, "아무개", LocalDateTime.now().minusMinutes(10), OrderStatus.ACCEPTED, cart1);

        Cart cart2 = new Cart();
        cart2.addItem(new OrderItem(menuLatte, Map.of(ogSize, optRegular, ogSyrup, optVanilla), 1));
        Order order2 = new Order(102, "아무개", LocalDateTime.now().minusMinutes(5), OrderStatus.PENDING, cart2);

        Cart cart3 = new Cart();
        cart3.addItem(new OrderItem(menuAmericano, Collections.emptyMap(), 2));
        Order order3 = new Order(103, "아무개", LocalDateTime.now().minusMinutes(2), OrderStatus.CANCELED, cart3);

        Cart cart4 = new Cart();
        cart4.addItem(new OrderItem(menuLatte, Collections.emptyMap(), 1));
        Order order4 = new Order(104, "아무개", LocalDateTime.now(), OrderStatus.PENDING, cart4);

        RegistryManager.OPTIONS.add(optRegular.id(), optRegular);
        RegistryManager.OPTIONS.add(optLarge.id(), optLarge);
        RegistryManager.OPTIONS.add(optAddShot.id(), optAddShot);
        RegistryManager.OPTIONS.add(optVanilla.id(), optVanilla);
        RegistryManager.OPTIONS.add(optHazelnut.id(), optHazelnut);

        RegistryManager.OPTION_GROUPS.add(ogSize.id(), ogSize);
        RegistryManager.OPTION_GROUPS.add(ogShot.id(), ogShot);
        RegistryManager.OPTION_GROUPS.add(ogSyrup.id(), ogSyrup);
        RegistryManager.MENUS.add(menuAmericano.id(), menuAmericano);
        RegistryManager.MENUS.add(menuLatte.id(), menuLatte);
        RegistryManager.CATEGORIES.add(cateCoffee.cateId(), cateCoffee);
        RegistryManager.ORDERS.add(String.valueOf(order1.orderId()), order1);
        RegistryManager.ORDERS.add(String.valueOf(order2.orderId()), order2);
        RegistryManager.ORDERS.add(String.valueOf(order3.orderId()), order3);
        RegistryManager.ORDERS.add(String.valueOf(order4.orderId()), order4);
    }
}