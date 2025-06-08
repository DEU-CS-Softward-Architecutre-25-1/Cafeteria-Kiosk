package common;

import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;

import java.util.*;

public class Cart {
    private final Reference2IntLinkedOpenHashMap<OrderItem> items = new Reference2IntLinkedOpenHashMap<>();

    public void addItem(OrderItem item) {
        items.merge(item, 1, Integer::sum);
    }

    public Map<OrderItem, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public int calculateCartTotal() {
        return items.reference2IntEntrySet().stream()
                .mapToInt(e -> e.getKey().getTotalPrice() * e.getIntValue())
                .sum();
    }

    public void increaseQuantity(OrderItem item) {
        items.merge(item, 1, Integer::sum);
    }

    public void clear() {
        items.clear();
    }
}
