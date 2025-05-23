package dev.qf.client;

import common.Order;
import common.OrderItem;
import common.OrderStatus;
import common.OrderService;
import common.Cart;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClientOrderService implements OrderService {
    private final List<Order> orders = new ArrayList<>();

    public ClientOrderService() {
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem("아메리카노", 1, "ICE"));
        items.add(new OrderItem("카페라떼", 1, "HOT"));
        items.add(new OrderItem("딸기케이크", 1, "조각"));

        Cart cart = new Cart(items);

        orders.add(new Order(1, "KIOSK-001", LocalDateTime.now(), OrderStatus.대기중, cart));
    }

    @Override
    public List<Order> getOrderList() {
        return orders;
    }

    @Override
    public void acceptOrder(int orderId) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.orderId() == orderId) {
                orders.set(i, order.withStatus(OrderStatus.수락됨));
            }
        }
    }

    @Override
    public void cancelOrder(int orderId) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.orderId() == orderId) {
                orders.set(i, order.withStatus(OrderStatus.취소됨));
            }
        }
    }
}
