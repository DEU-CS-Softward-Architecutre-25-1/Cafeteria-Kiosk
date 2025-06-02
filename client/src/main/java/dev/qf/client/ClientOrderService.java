package dev.qf.client;

import common.*;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.*;

public class ClientOrderService implements OrderService {
    private final List<Order> orders = new ArrayList<>();

    public ClientOrderService() {
        // 옵션 및 옵션 그룹 정의 (기존과 동일)
        Option tempHot = new Option("temp_hot", "뜨겁게", 0);
        Option tempCold = new Option("temp_cold", "차갑게", 0);
        // tempGroup은 "온도"라는 이름을 가짐
        OptionGroup tempGroup = new OptionGroup("temp", "온도", true, List.of(tempHot, tempCold));

        Option iceNone = new Option("ice_none", "없음", 0);
        Option iceRegular = new Option("ice_regular", "기본", 0);
        Option iceMore = new Option("ice_more", "많이", 0);
        // iceGroup은 "얼음 양"이라는 이름을 가짐
        OptionGroup iceGroup = new OptionGroup("ice", "얼음 양", false, List.of(iceNone, iceRegular, iceMore));

        Menu americano = new Menu(
                "menu001",
                "아메리카노",
                4000,
                "COFFEE",
                Path.of("images/americano.jpg"),
                "에티오피아 원두의 산뜻함",
                List.of(tempGroup, iceGroup) // 아메리카노 메뉴에 옵션 그룹 연결
        );

        // selectedOptions 맵의 타입을 Map<OptionGroup, Option>으로 변경
        Map<OptionGroup, Option> selectedOptionsForAmericano = new HashMap<>();
        // 키로 실제 OptionGroup 객체를 사용
        selectedOptionsForAmericano.put(tempGroup, tempHot);   // tempGroup 객체를 키로 사용
        selectedOptionsForAmericano.put(iceGroup, iceNone);    // iceGroup 객체를 키로 사용

        // 이제 올바른 타입의 맵을 OrderItem 생성자에 전달
        OrderItem item1 = new OrderItem(americano, selectedOptionsForAmericano, 2);

        // Cart 생성 시에도 item1은 이미 올바른 selectedOptions를 가짐
        Cart cart1 = new Cart(Map.of(item1, 1)); // item1의 수량은 생성자에서 이미 2로 지정됨
        // Cart의 Map.of(item1, 1)은 이 OrderItem을 1개 담는다는 의미
        // 만약 OrderItem 자체의 수량을 Cart에서 관리한다면
        // OrderItem 생성 시 수량은 1로 하고 Cart에 수량을 넣어야 함.
        // 현재 구조는 OrderItem에 수량이 있고, Cart는 (OrderItem, 이_OrderItem_묶음_개수) 형태.

        orders.add(new Order(1, "KIOSK-001", LocalDateTime.now(), OrderStatus.PENDING, cart1));

        // 추가 예시: 다른 메뉴와 옵션 (필요한 경우)
        /*
        Option shotDefault = new Option("shot_default", "기본", 0);
        Option shot1 = new Option("shot_1", "1샷 추가", 500);
        OptionGroup shotGroup = new OptionGroup("shot", "샷 추가", false, List.of(shotDefault, shot1));

        Menu cafeLatte = new Menu(
                "menu002",
                "카페라떼",
                4500,
                "COFFEE",
                Path.of("images/cafelatte.jpg"),
                "부드러운 우유와 에스프레소의 조화",
                List.of(tempGroup, shotGroup) // 카페라떼는 온도와 샷추가 옵션 그룹을 가짐
        );

        Map<OptionGroup, Option> selectedOptionsForLatte = new HashMap<>();
        selectedOptionsForLatte.put(tempGroup, tempCold); // 예: 차가운 라떼
        selectedOptionsForLatte.put(shotGroup, shot1);    // 예: 1샷 추가

        OrderItem item2 = new OrderItem(cafeLatte, selectedOptionsForLatte, 1);
        Cart cart2 = new Cart(Map.of(item2, 1));
        orders.add(new Order(2, "KIOSK-002", LocalDateTime.now(), OrderStatus.PENDING, cart2));
        */
    }

    @Override
    public List<Order> getOrderList() {
        return Collections.unmodifiableList(orders); // 외부에서 리스트 직접 수정을 방지
    }

    @Override
    public void acceptOrder(int orderId) {
        updateOrderStatus(orderId, OrderStatus.ACCEPTED);
    }

    @Override
    public void cancelOrder(int orderId) {
        updateOrderStatus(orderId, OrderStatus.CANCELED);
    }

    @Override
    public void updateOrderStatus(int orderId, OrderStatus newStatus) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.orderId() == orderId) {
                // Order 레코드가 withStatus 같은 변경 메서드를 제공한다고 가정
                orders.set(i, order.withStatus(newStatus));
                return; // 해당 주문을 찾았으면 루프 종료
            }
        }
        // 주문을 찾지 못한 경우에 대한 처리 (예: 로그 남기기 또는 예외 발생)
        System.err.println("주문 ID " + orderId + "를 찾을 수 없습니다.");
    }
}
