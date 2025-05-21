package common;

import java.util.*;

public class Cart {
    private final Map<OrderItem, Integer> items = new LinkedHashMap<>();

    public void addItem(OrderItem item) {
        items.merge(item, 1, Integer::sum);
    }

    public Map<OrderItem, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public int calculateCartTotal() {
        return items.entrySet().stream()
                .mapToInt(e -> e.getKey().getTotalPrice() * e.getValue())
                .sum();
    }

    public void increaseQuantity(OrderItem item) {
        items.merge(item, 1, Integer::sum);
    }
}
