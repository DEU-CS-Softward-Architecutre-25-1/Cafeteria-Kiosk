package dev.qf.client;

import common.Menu;
import common.Option;
import common.OptionGroup;
import common.OrderItem;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.Map;

public class OptionSelectionController {

    /**
     * 선택된 옵션들이 메뉴의 옵션 그룹 규칙을 만족하는지 검증
     * @param selectedOptions 선택된 옵션들 (OptionGroup을 키로, Option을 값으로)
     * @param rules 메뉴의 옵션 그룹 규칙들
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidOption(Map<OptionGroup, Option> selectedOptions, List<OptionGroup> rules) {
        for (OptionGroup group : rules) {
            Option selectedOption = selectedOptions.get(group);

            // 필수 옵션 그룹인데 선택되지 않은 경우
            if (group.required() && selectedOption == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "필수 옵션 그룹 '" + group.name() + "'이(가) 선택되지 않았습니다.",
                        "옵션 선택 오류",
                        JOptionPane.WARNING_MESSAGE
                );
                return false;
            }

            // 선택된 옵션이 해당 그룹에 속하지 않는 경우
            if (selectedOption != null && !group.options().contains(selectedOption)) {
                JOptionPane.showMessageDialog(
                        null,
                        "선택된 옵션 '" + selectedOption.name() + "'이(가) 옵션 그룹 '" + group.name() + "'에 속하지 않습니다.",
                        "옵션 선택 오류",
                        JOptionPane.WARNING_MESSAGE
                );
                return false;
            }
        }
        return true;
    }

    /**
     * 검증된 옵션들로 OrderItem 생성
     * @param menu 메뉴
     * @param selectedOptions 선택된 옵션들 (이미 검증된 상태)
     * @param quantity 수량
     * @return 생성된 OrderItem
     */
    public OrderItem createOrderItem(Menu menu, Map<OptionGroup, Option> selectedOptions, int quantity) {
        // 옵션 검증
        if (!isValidOption(selectedOptions, menu.optionGroup())) {
            throw new IllegalArgumentException("유효하지 않은 옵션이 선택되었습니다.");
        }

        return new OrderItem(menu, selectedOptions, quantity);
    }

    /**
     * 필수 옵션들이 모두 선택되었는지 확인
     * @param selectedOptions 선택된 옵션들
     * @param rules 옵션 그룹 규칙들
     * @return 모든 필수 옵션이 선택되었으면 true
     */
    public boolean hasAllRequiredOptions(Map<OptionGroup, Option> selectedOptions, List<OptionGroup> rules) {
        for (OptionGroup group : rules) {
            if (group.required() && !selectedOptions.containsKey(group)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 선택 가능한 옵션들만 필터링
     * @param selectedOptions 모든 선택된 옵션들
     * @param availableGroups 사용 가능한 옵션 그룹들
     * @return 유효한 옵션들만 포함된 Map
     */
    public Map<OptionGroup, Option> filterValidOptions(Map<OptionGroup, Option> selectedOptions, List<OptionGroup> availableGroups) {
        return selectedOptions.entrySet().stream()
                .filter(entry -> availableGroups.contains(entry.getKey()))
                .filter(entry -> entry.getKey().options().contains(entry.getValue()))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}