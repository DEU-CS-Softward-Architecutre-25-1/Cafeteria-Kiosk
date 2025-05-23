package common;

import java.util.List;

public record Cart(List<OrderItem> items) {
    public static final Cart EMPTY = new Cart(List.of());
}
