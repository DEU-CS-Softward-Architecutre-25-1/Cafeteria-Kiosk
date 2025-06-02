package dev.qf.client;

import common.*;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.*;

public class ClientOrderService implements OrderService {
    private final List<Order> orders = new ArrayList<>();

    //테스트 위한 더미데이터 생성
    public ClientOrderService() {
        Option tempHot = new Option("temp_hot", "뜨겁게", 0);
        Option tempCold = new Option("temp_cold", "차갑게", 0);
        OptionGroup tempGroup = new OptionGroup("temp", "온도", true, List.of(tempHot, tempCold));

        Option iceNone = new Option("ice_none", "없음", 0);
        Option iceRegular = new Option("ice_regular", "기본", 0);
        Option iceMore = new Option("ice_more", "많이", 0);
        OptionGroup iceGroup = new OptionGroup("ice", "얼음 양", false, List.of(iceNone, iceRegular, iceMore));

        Menu americano = new Menu(
                "menu001",
                "아메리카노",
                4000,
                "커피",
                Path.of("images/americano.jpg"),
                "에티오피아 원두의 산뜻함",
                List.of(tempGroup, iceGroup)
        );

        Map<String, Option> selectedOptions = new HashMap<>();
        selectedOptions.put("온도", tempHot);
        selectedOptions.put("얼음 양", iceNone);

        OrderItem item1 = new OrderItem(
                americano,
                selectedOptions,
                2
        );

        Cart cart1 = new Cart(Map.of(item1, 1));

        orders.add(new Order(
                1,
                "KIOSK-001",
                LocalDateTime.now(),
                OrderStatus.PENDING,
                cart1
        ));

        Option tempWarm = new Option("temp_warm", "미지근하게", 0);
        OptionGroup latteTempGroup = new OptionGroup("temp", "온도", true, List.of(tempHot, tempWarm, tempCold));
        Option shotDefault = new Option("shot_default", "기본", 0);
        Option shot1 = new Option("shot_1", "1샷 추가", 500);
        OptionGroup shotGroup = new OptionGroup("shot", "샷 추가", false, List.of(shotDefault, shot1));

        Menu latte = new Menu(
                "menu002",
                "카페라떼",
                5000,
                "커피",
                Path.of("images/latte.jpg"),
                "부드러운 우유와 커피의 조화",
                List.of(latteTempGroup, shotGroup)
        );

        Map<String, Option> selectedOptions2 = new HashMap<>();
        selectedOptions2.put("온도", tempCold);
        selectedOptions2.put("샷 추가", shot1);

        OrderItem item2 = new OrderItem(
                latte,
                selectedOptions2,
                1
        );

        Cart cart2 = new Cart(Map.of(item2, 1));

        orders.add(new Order(
                2,
                "KIOSK-002",
                LocalDateTime.now().minusMinutes(10),
                OrderStatus.ACCEPTED,
                cart2
        ));
    }

    @Override
    public List<Order> getOrderList() {
        return orders;
    }

    @Override
    public void acceptOrder(int orderId) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.orderId() == orderId) {
                orders.set(i, order.withStatus(OrderStatus.ACCEPTED));
            }
        }
    }

    @Override
    public void cancelOrder(int orderId) {
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            if (order.orderId() == orderId) {
                orders.set(i, order.withStatus(OrderStatus.CANCELED));
            }
        }
    }
}
