package common;

import java.util.List;
import java.util.Map;

public class OptionSelectionController {
    public boolean isValidOption(Map<String, String> selectedOptions, List<OptionGroup> rules) {
        for (OptionGroup group : rules) {
            if (group.required() && !selectedOptions.containsKey(group.name())) {
                return false;
            }
        }
        return true;
    }

    public OrderItem createOrderItem(Menu menu, Map<String, Option> selectedOptions, int quantity) {
        return new OrderItem(menu, selectedOptions, quantity);
    }
}