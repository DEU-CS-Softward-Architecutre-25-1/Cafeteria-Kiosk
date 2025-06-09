package common;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> getOrderList();
    void acceptOrder(int orderId);
    void cancelOrder(int orderId);
    void updateOrderStatus(int orderId, OrderStatus newStatus);
    Optional<Order> getOrderById(int orderId);
}