package common.registry;

import common.Order;
import common.network.SynchronizeData;

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
        try {
            if (isFrozen()) throw new IllegalStateException("Registry is frozen");
            lock.lock();
            String wrappedId = String.valueOf(order.orderId());
            this.rawIndexToEntry.put(order.orderId(), order);
            this.idToEntry.put(wrappedId, order);
            this.entryToId.put(order, wrappedId);
            this.ITEMS.add(order);
            this.entryToRawIndex.put(order, order.orderId());
            return order;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void addAll(List<SynchronizeData<?>> dataList) {
        try {
            if (this.isFrozen()) throw new IllegalStateException("Registry is frozen");
            lock.lock();

            dataList.forEach(data -> {
                if (!(data instanceof Order order)) {
                    this.LOGGER.warn("Entry must be an instance of Order");
                    return;
                }
                if (this.rawIndexToEntry.containsKey(order.orderId())) {
                    this.LOGGER.info("Order with id {} already exists. overriding {}", order.orderId(), this.rawIndexToEntry.get(order.orderId()));
                }

                this.rawIndexToEntry.put(order.orderId(), order);
                this.idToEntry.put(String.valueOf(order.orderId()), order);
                this.entryToId.put(order, String.valueOf(order.orderId()));
                this.ITEMS.add(order);
                this.entryToRawIndex.put(order, order.orderId());
            });

        } finally {
            lock.unlock();
        }
    }

}
