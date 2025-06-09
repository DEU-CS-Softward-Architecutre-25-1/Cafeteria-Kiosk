package common.registry;

import common.Order;
import common.network.SynchronizeData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderRegistry extends SimpleRegistry<Order> {
    public OrderRegistry() {
        super("order", Order.CODEC, Order.class);
    }

    @Override
    public Order add(String id, SynchronizeData<?> entry) {
        if (isFrozen()) throw new IllegalStateException("Registry is frozen");
        if (!(entry instanceof Order order)) {
            throw new IllegalArgumentException("Entry must be an instance of Order");
        }
        return this.addOrder(order);
    }

    public Order getOrderById(int orderId) {
        return this.rawIndexToEntry.get(orderId);
    }

    public Order addOrder(Order order) {
        lock.lock();
        try {
            if (isFrozen()) throw new IllegalStateException("Registry is frozen");

            /*
            클라이언트가 서버로부터 주문 정보를 받아 addOrder(order) 메소드를 호출할 때
            이때, 데이터를 모두 받은 후 레지스트리가 'frozen(동결)' 상태라고 가정.
            디버그 해보니 new IllegalStateException("Registry is frozen") 예외가 발생.
            프로그램 실행 흐름은 즉시 lock.lock() 라인을 건너뛰고 finally 블록으로 점프.
            finally 블록에서 lock.unlock()을 실행.
            스레드는 lock.lock()을 호출한 적이 없으므로, 잠그지도 않은 락을 풀려고 시도.
            결과적으로 IllegalMonitorStateException이 발생하면서 프로그램이 비정상 종료.
            */

            //기존에 같은 주문 ID를 가진 주문이 있는지 찾아 제거.
            // List.removeIf를 사용하여 같은 orderId를 가진 기존 항목을 삭제.
            this.ITEMS.removeIf(existingOrder -> existingOrder.orderId() == order.orderId());

            //이제 리스트에는 해당 orderId가 없으므로, 안심하고 새 주문을 추가.
            //항상 주문 ID당 하나의 최신 상태만 유지.
            this.ITEMS.add(order);

            // 나머지 Map 데이터들은 key-value 형태이므로 자동으로 덮어쓰기.
            String wrappedId = String.valueOf(order.orderId());
            this.rawIndexToEntry.put(order.orderId(), order);
            this.idToEntry.put(wrappedId, order);
            this.entryToId.put(order, wrappedId);
            this.entryToRawIndex.put(order, order.orderId());

            return order;
        } finally {
            lock.unlock();
        }
    }

    // addOrder 메소드와 동일한 이유로 수정.
    @Override
    public void addAll(List<SynchronizeData<?>> dataList) {
        lock.lock();
        try {
            if (this.isFrozen()) throw new IllegalStateException("Registry is frozen");

            dataList.forEach(data -> {
                if (!(data instanceof Order order)) {
                    this.LOGGER.warn("Entry must be an instance of Order");
                    return;
                }

                // addOrder와 동일하게 중복을 방지하기 위해 기존 항목을 먼저 제거.
                this.ITEMS.removeIf(existingOrder -> existingOrder.orderId() == order.orderId());
                this.ITEMS.add(order);

                this.rawIndexToEntry.put(order.orderId(), order);
                this.idToEntry.put(String.valueOf(order.orderId()), order);
                this.entryToId.put(order, String.valueOf(order.orderId()));
                this.entryToRawIndex.put(order, order.orderId());
            });

        } finally {
            lock.unlock();
        }
    }

   // 주문목록 내림차순
    @Override
    public List<Order> getAll() {
        List<Order> orderList = new ArrayList<>(this.ITEMS);
        orderList.sort(Comparator.comparingInt(Order::orderId).reversed());
        return orderList;
    }
}
