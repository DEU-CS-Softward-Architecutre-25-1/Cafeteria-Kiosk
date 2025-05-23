package common;

import java.time.LocalDateTime;
import java.util.List;

public record Order(
        int orderId,
        String customer,
        LocalDateTime orderTime,
        OrderStatus status,
        Cart cart
) {
    public static final Order EMPTY = new Order(-1, "UNKNOWN", LocalDateTime.MIN, OrderStatus.대기중, Cart.EMPTY);

    public Order(int orderId, String customer, LocalDateTime orderTime, OrderStatus status) {
        this(orderId, customer, orderTime, status, new Cart(List.of()));
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.orderId, this.customer, this.orderTime, newStatus, this.cart);
    }
}
