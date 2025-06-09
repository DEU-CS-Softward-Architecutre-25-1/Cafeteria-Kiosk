package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Cart.java는 내부에 Map<OrderItem, Integer> 형태의 데이터를 가지는데
이것을 JSON 형태로 변환(직렬화)하려고 할 때, OrderItem을 JSON의 키로 사용하려고 시도하여
JSON 직렬화와 호환되지 않아 에러가 발생하여 수정
 */

public class Cart {
    public static Codec<Cart> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            OrderItem.CODEC.listOf().fieldOf("items").forGetter(Cart::getItems)
    ).apply(instance, Cart::new)));

    public static final Cart EMPTY = new Cart() {
        @Override
        public void addItem(OrderItem item) {
            throw new UnsupportedOperationException("Cannot add items to empty cart");
        }
    };
    private final List<OrderItem> items;

    public Cart() {
        this(new ArrayList<>());
    }

    public Cart(List<OrderItem> items) {
        this.items = new ArrayList<>(items);
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int calculateCartTotal() {
        return items.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }
}