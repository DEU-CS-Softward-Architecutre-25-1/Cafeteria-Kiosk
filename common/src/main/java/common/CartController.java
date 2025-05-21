package common;

public class CartController {
    private final Cart cart;

    public CartController(Cart cart) {
        this.cart = cart;
    }

    public void addItemToCart(OrderItem item) {
        cart.addItem(item);
    }
}