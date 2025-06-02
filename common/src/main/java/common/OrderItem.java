package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Objects;

public class OrderItem {
    public static final Codec<OrderItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Menu.CODEC.fieldOf("menuItem").forGetter(OrderItem::getMenuItem),
                    Codec.unboundedMap(OptionGroup.CODEC, Option.CODEC).fieldOf("selectedItems").forGetter(OrderItem::getSelectedOptions),
                    Codec.INT.fieldOf("quantity").forGetter(OrderItem::getQuantity)
            ).apply(instance, OrderItem::new)
    );
    private final Menu menuItem;
    private final Map<OptionGroup, Option> selectedOptions;
    private final int quantity;
    private final int totalPrice;

    public OrderItem(Menu menuItem, Map<OptionGroup, Option> selectedOptions, int quantity) {
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

    public Map<OptionGroup, Option> getSelectedOptions() {
        return selectedOptions;
    }

    public int getQuantity() {
        return quantity;
    }
}