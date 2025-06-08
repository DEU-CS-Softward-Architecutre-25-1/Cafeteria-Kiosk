package dev.qf.client;

import common.*;
import common.network.packet.UpdateDataPacket;
import dev.qf.client.network.KioskNettyClient;
import common.util.KioskLoggerFactory;
import common.registry.RegistryManager;
import common.registry.Registry;
import common.network.SynchronizeData;

import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import io.netty.channel.ChannelFuture;

public class ClientOrderService implements OrderService {
    private final List<Order> orders = Collections.synchronizedList(new ArrayList<>());
    private KioskNettyClient kioskClient;
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    private OwnerMainUI ownerMainUIInstance;

    public ClientOrderService() {
    }

    public void setKioskClient(KioskNettyClient client) {
        this.kioskClient = client;
    }

    public void setOwnerMainUI(OwnerMainUI ownerMainUI) {
        this.ownerMainUIInstance = ownerMainUI;
    }

    public void refreshOwnerMainUIOrders() {
        if (ownerMainUIInstance != null) {
            ownerMainUIInstance.loadOrderData();
            LOGGER.info("UI: Requesting OwnerMainUI to refresh order data.");
        }
    }

    @Override
    public List<Order> getOrderList() {
        if (kioskClient != null && kioskClient.isConnected()) {
            UpdateDataPacket.RequestDataC2SPacket requestOrdersPacket = new UpdateDataPacket.RequestDataC2SPacket("orders");
            ChannelFuture future = kioskClient.sendSerializable(requestOrdersPacket.getPacketId(), requestOrdersPacket);
            if (future != null) {
                future.addListener(f -> {
                    if (f.isSuccess()) {
                        LOGGER.info("서버에 주문 목록 요청 성공: 'orders' registryId.");
                    } else {
                        LOGGER.error("서버에 주문 목록 요청 실패: {}", f.cause().getMessage());
                    }
                });
            } else {
                LOGGER.error("sendSerializable for 'orders' request returned null. Packet might not have been sent.");
            }
        } else {
            LOGGER.error("KioskNettyClient가 초기화되지 않아 주문 목록을 요청할 수 없습니다.");
        }

        Registry<?> ordersRegistry = RegistryManager.getAsId("orders");
        if (ordersRegistry != null) {
            try {
                @SuppressWarnings("unchecked")
                List<Order> registeredOrders = (List<Order>) ordersRegistry.getAll();
                synchronized (orders) {
                    orders.clear();
                    orders.addAll(registeredOrders);
                }
                return Collections.unmodifiableList(orders);
            } catch (ClassCastException e) {
                LOGGER.error("Registry 'orders' contains non-Order elements. Type mismatch.", e);
                return Collections.emptyList();
            }
        } else {
            LOGGER.warn("Registry 'orders' not found in RegistryManager. Returning empty list.");
            return Collections.emptyList();
        }
    }

    @Override
    public void acceptOrder(int orderId) {
        sendOrderStatusUpdateUsingExistingPacket(orderId, OrderStatus.ACCEPTED);
    }

    @Override
    public void cancelOrder(int orderId) {
        sendOrderStatusUpdateUsingExistingPacket(orderId, OrderStatus.CANCELED);
    }

    @Override
    public void updateOrderStatus(int orderId, OrderStatus newStatus) {
        if (kioskClient != null && kioskClient.isConnected()) {
            sendOrderStatusUpdateUsingExistingPacket(orderId, newStatus);
        } else {
            LOGGER.error("KioskNettyClient가 연결되지 않아 주문 상태를 변경할 수 없습니다.");
        }
    }

    private void sendOrderStatusUpdateUsingExistingPacket(int orderId, OrderStatus status) {
        if (kioskClient != null && kioskClient.isConnected()) {
            String requestIdentifier = String.format("order_status_update:%d:%s", orderId, status.name());
            UpdateDataPacket.RequestDataC2SPacket packet = new UpdateDataPacket.RequestDataC2SPacket(requestIdentifier);

            ChannelFuture future = kioskClient.sendSerializable(packet.getPacketId(), packet);
            if (future != null) {
                future.addListener(f -> {
                    if (f.isSuccess()) {
                        LOGGER.info("주문 ID {}의 상태를 {}로 변경 요청 성공.", orderId, status);
                    } else {
                        LOGGER.error("주문 ID {}의 상태 변경 요청 실패: {}", orderId, f.cause().getMessage());
                    }
                });
            } else {
                LOGGER.error("sendSerializable for order status update returned null. Packet might not have been sent.");
            }
        } else {
            LOGGER.error("KioskNettyClient가 연결되지 않았습니다. 주문 상태 변경 요청을 보낼 수 없습니다.");
        }
    }

    private void updateLocalOrderStatus(int orderId, OrderStatus newStatus) {
        synchronized (orders) {
            for (int i = 0; i < orders.size(); i++) {
                Order order = orders.get(i);
                if (order.orderId() == orderId) {
                    orders.set(i, order.withStatus(newStatus));
                    LOGGER.info("로컬에서 주문 ID {}의 상태를 {}로 업데이트 완료.", orderId, newStatus);
                    return;
                }
            }
            LOGGER.error("오류: 로컬에서 주문 ID {}를 찾을 수 없어 상태 업데이트 실패.", orderId);
        }
        refreshOwnerMainUIOrders();
    }
}