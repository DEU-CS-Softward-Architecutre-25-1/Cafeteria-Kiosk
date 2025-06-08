package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;

import java.util.*;

public class Cart {
    public static Codec<Cart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(OrderItem.CODEC, Codec.INT).fieldOf("items").forGetter(Cart::getItems)
    ).apply(instance, Cart::new));

    public static final Cart EMPTY = new Cart() {
        @Override
        public void addItem(OrderItem item) {
            throw new UnsupportedOperationException("Cannot add items to empty cart");
        }
    };
    private final Reference2IntLinkedOpenHashMap<OrderItem> items;

    public Cart() {
        this(new Reference2IntLinkedOpenHashMap<>());
    }

    public Cart(Map<OrderItem, Integer> override) {
        items = new Reference2IntLinkedOpenHashMap<>(override);
    }

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
}
