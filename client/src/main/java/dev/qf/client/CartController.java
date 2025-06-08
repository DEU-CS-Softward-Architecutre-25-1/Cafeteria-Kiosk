package dev.qf.client;

import common.Cart;
import common.OrderItem;

public class CartController {
    private final Cart cart;

    public CartController(Cart cart) {
        this.cart = cart;
    }

    public void addItemToCart(OrderItem item) {
        // 이미 같은 항목이 있으면 수량 증가
        OrderItem existing = findMatchingItem(item);
        if (existing != null) {
            cart.increaseQuantity(existing);
        } else {
            cart.addItem(item);
        }
    }

    public OrderItem findMatchingItem(OrderItem target) {
        for (OrderItem item : cart.getItems().keySet()) {
            // 메뉴가 같은지 확인
            boolean sameMenu = item.getMenuItem().equals(target.getMenuItem());

            // 옵션 Map이 완전히 같은지 확인
            boolean sameOptions = item.getSelectedOptions().equals(target.getSelectedOptions());

            if (sameMenu && sameOptions) {
                return item;
            }
        }
        return null;
    }

    public int getCartTotal() {
        return cart.calculateCartTotal();
    }
}