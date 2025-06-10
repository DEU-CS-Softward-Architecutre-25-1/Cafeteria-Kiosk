package common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class OrderItem {
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    public static final Codec<OrderItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Menu.SYNC_CODEC.fieldOf("menuItem").forGetter(OrderItem::getMenuItem),
                    Codec.unboundedMap(OptionGroup.CODEC, Option.CODEC).fieldOf("selectedItems").forGetter(OrderItem::getSelectedOptions),
                    Codec.INT.fieldOf("quantity").forGetter(OrderItem::getQuantity)
            ).apply(instance, (menuItem, selectedOptions, quantity) -> {
                LOGGER.info("DEBUG: OrderItem Codec: Successfully decoded OrderItem for menu: {}, quantity: {}", menuItem.name(), quantity);
                return new OrderItem(menuItem, selectedOptions, quantity);
            })
    );

    private final Menu menuItem;
    private final Map<OptionGroup, Option> selectedOptions;
    private final int quantity;
    private final int totalPrice;

    public OrderItem(Menu menuItem, Map<OptionGroup, Option> selectedOptions, int quantity) {
        this.menuItem = menuItem;
        this.selectedOptions = selectedOptions != null ? Map.copyOf(selectedOptions) : Map.of();
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
            selectedOptions.forEach((group, opt) ->
                    sb.append(group.name()).append(": ").append(opt.name()).append(", "));
            if (sb.length() > sb.lastIndexOf(", ") && sb.lastIndexOf(", ") > 0) {
                sb.setLength(sb.length() - 2);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public Menu getMenuItem() {
        return menuItem;
    }

    public Map<OptionGroup, Option> getSelectedOptions() {
        return Collections.unmodifiableMap(selectedOptions);
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity &&
                menuItem.equals(orderItem.menuItem) &&
                selectedOptions.equals(orderItem.selectedOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuItem, selectedOptions, quantity);
    }

}