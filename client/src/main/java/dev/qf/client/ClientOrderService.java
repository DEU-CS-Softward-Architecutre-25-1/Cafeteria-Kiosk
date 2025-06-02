package dev.qf.client;

import common.*;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.*;

public class ClientOrderService implements OrderService {
    private final List<Order> orders = new ArrayList<>();

    public ClientOrderService() {
        Option tempHot = new Option("temp_hot", "뜨겁게", 0);
        Option tempCold = new Option("temp_cold", "차갑게", 0);
        OptionGroup tempGroup = new OptionGroup("temp", "온도", true, List.of(tempHot, tempCold));

        Option iceNone = new Option("ice_none", "없음", 0);
        Option iceRegular = new Option("ice_regular", "기본", 0);
        Option iceMore = new Option("ice_more", "많이", 0);
        OptionGroup iceGroup = new OptionGroup("ice", "얼음 양", false, List.of(iceNone, iceRegular, iceMore));

        Menu americano = new Menu(
                "menu001",
                "아메리카노",
                4000,
                "COFFEE",
                Path.of("images/americano.jpg"),
                "에티오피아 원두의 산뜻함",
                List.of(tempGroup, iceGroup)
        );

        Map<String, Option> selectedOptions1 = new HashMap<>();
        selectedOptions1.put(tempGroup.name(), tempHot);
        selectedOptions1.put(iceGroup.name(), iceNone);

        OrderItem item1 = new OrderItem(americano, selectedOptions1, 2);
        Cart cart1 = new Cart(Map.of(item1, 1));

        orders.add(new Order(1, "KIOSK-001", LocalDateTime.now(), OrderStatus.PENDING, cart1));
    }

    @Override
    public List<Order> getOrderList() {
        return orders;
    }

    @Override
    public void acceptOrder(int orderId) {
        updateOrderStatus(orderId, OrderStatus.ACCEPTED);
    }

    @Override
    public void cancelOrder(int orderId) {
        updateOrderStatus(orderId, OrderStatus.CANCELED);
    }

    @Override
    public void updateOrderStatus(int orderId, OrderStatus newStatus) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.orderId() == orderId) {
                orders.set(i, order.withStatus(newStatus));
            }
        }
    }
}
