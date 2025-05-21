package common;

import java.util.Map;
import java.util.Objects;

public class OrderItem {
    private final Menu menuItem;
    private final Map<String, Option> selectedOptions;
    private final int quantity;
    private final int totalPrice;

    public OrderItem(Menu menuItem, Map<String, Option> selectedOptions, int quantity) {
        this.menuItem = menuItem;
        this.selectedOptions = selectedOptions;
        this.quantity = quantity;
        this.totalPrice = calculateTotalPrice();
    }

    private int calculateTotalPrice() {
        int optionsCost = selectedOptions.values().stream()
                .mapToInt(Option::extraCost)
                .sum();
        return (menuItem.price() + optionsCost) * quantity;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public String getOrderDescription() {
        StringBuilder sb = new StringBuilder(menuItem.name());
        if (!selectedOptions.isEmpty()) {
            sb.append(" (");
            selectedOptions.forEach((key, opt) ->
                    sb.append(key).append(": ").append(opt.name()).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append(")");
        }
        return sb.toString();
    }

    public Menu getMenuItem() {
        return menuItem;
    }

    public Map<String, Option> getSelectedOptions() {
        return selectedOptions;
    }

    public int getQuantity() {
        return quantity;
    }
}
