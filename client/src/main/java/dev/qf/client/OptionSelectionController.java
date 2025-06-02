package dev.qf.client;

import common.Menu;
import common.Option;
import common.OptionGroup;
import common.OrderItem;

import java.util.HashMap; // HashMap import 추가
import java.util.List;
import java.util.Map;

public class OptionSelectionController {
    public boolean isValidOption(Map<String, String> selectedOptions, List<OptionGroup> rules) {
         /*이 메서드는 selectedOptions의 키가 OptionGroup의 name과 일치한다고 가정.
         현재 selectedOptions의 값 타입은 String으로 되어있는데
         만약 Option 객체 자체를 저장해야 한다면 Map<String, Option>으로 변경하는 것을 고려해야 함.
         현재는 String 키(OptionGroup 이름)의 존재 유무만 확인.*/
        for (OptionGroup group : rules) {
            if (group.required() && !selectedOptions.containsKey(group.name())) {
                System.err.println("필수 옵션 그룹 '" + group.name() + "'이(가) 선택되지 않았습니다.");
                return false;
            }
        }
        return true;
    }

    // selectedOptions 파라미터는 여전히 Map<String, Option>으로 받되, 내부에서 변환.
    public OrderItem createOrderItem(Menu menu, Map<String, Option> stringKeyedSelectedOptions, int quantity) {
        Map<OptionGroup, Option> groupKeyedSelectedOptions = new HashMap<>();
        List<OptionGroup> availableOptionGroups = menu.optionGroup(); // 메뉴의 전체 옵션 그룹 목록

        for (Map.Entry<String, Option> entry : stringKeyedSelectedOptions.entrySet()) {
            String optionGroupName = entry.getKey(); // 예: "온도"
            Option chosenOption = entry.getValue();    // 예: tempHot (Option 객체)

            // 메뉴의 옵션 그룹 목록에서 이름이 일치하는 OptionGroup 객체를 찾음.
            OptionGroup actualOptionGroup = null;
            for (OptionGroup og : availableOptionGroups) {
                if (og.name().equals(optionGroupName)) {
                    actualOptionGroup = og;
                    break;
                }
            }

            if (actualOptionGroup != null) {
                // 실제 OptionGroup 객체를 키로 사용하여 맵에 추가
                groupKeyedSelectedOptions.put(actualOptionGroup, chosenOption);
            } else {
                // 오류 처리: 해당 이름을 가진 OptionGroup을 메뉴에서 찾을 수 없음
                // 이 경우는 로직상 발생하기 어렵거나, 데이터 불일치를 의미할 수 있음
                System.err.println("경고: 옵션 그룹 '" + optionGroupName + "'을(를) 메뉴 '" + menu.name() + "'에서 찾을 수 없습니다.");
            }
        }

        // 변환된 Map<OptionGroup, Option> 타입의 맵을 사용하여 OrderItem 생성
        return new OrderItem(menu, groupKeyedSelectedOptions, quantity);
    }
}
