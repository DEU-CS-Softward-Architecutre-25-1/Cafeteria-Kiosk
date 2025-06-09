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
        if (!(entry instanceof Order order)) {
            throw new IllegalArgumentException("Entry must be an instance of Order");
        }
        return this.addOrder(order);
    }

    public Order getOrderById(int orderId) {
        return this.rawIndexToEntry.get(orderId);
    }

    public Order addOrder(Order order) {
        if (isFrozen()) throw new IllegalStateException("Registry is frozen");
        try {
            lock.writeLock().lock();

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
            lock.writeLock().unlock();
        }
    }

    // addOrder 메소드와 동일한 이유로 수정.
    @Override
    public void addAll(List<SynchronizeData<?>> dataList) {
        if (this.isFrozen()) throw new IllegalStateException("Registry is frozen");
        try {
            lock.writeLock().lock();
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
            lock.writeLock().unlock();
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
