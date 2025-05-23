package common;

import java.util.List;

public interface OrderService {
    List<Order> getOrderList();
    void acceptOrder(int orderId);
    void cancelOrder(int orderId);
}
