package dev.qf.client;

import common.*;
import common.network.packet.OrderStatusChangedC2SPacket;
import common.network.packet.UpdateDataPacket;
import dev.qf.client.network.KioskNettyClient;
import common.util.KioskLoggerFactory;
import common.registry.RegistryManager;
import common.network.Connection;
import common.util.Container;

import java.util.*;

import org.slf4j.Logger;
import io.netty.channel.ChannelFuture;

public class ClientOrderService implements OrderService {
    private KioskNettyClient kioskClient;
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    private OwnerMainUI ownerMainUIInstance;

    public ClientOrderService() {
        kioskClient = (KioskNettyClient) Container.get(Connection.class);
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

    public void requestOrderListUpdate() {
        if (kioskClient != null && kioskClient.isConnected()) {
            UpdateDataPacket.RequestDataC2SPacket requestOrdersPacket = new UpdateDataPacket.RequestDataC2SPacket(RegistryManager.ORDERS.getRegistryId());
            ChannelFuture future = kioskClient.sendSerializable(requestOrdersPacket);
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
            LOGGER.error("KioskNettyClient가 연결되지 않아 주문 목록을 요청할 수 없습니다.");
        }
    }

    @Override
    public List<Order> getOrderList() {
        return RegistryManager.ORDERS.getAll();
    }

    @Override
    public void acceptOrder(int orderId) {
        sendOrderStatusUpdated(orderId, OrderStatus.ACCEPTED);
    }

    @Override
    public void cancelOrder(int orderId) {
        sendOrderStatusUpdated(orderId, OrderStatus.CANCELED);
    }

    @Override
    public void updateOrderStatus(int orderId, OrderStatus newStatus) {
        if (kioskClient != null && kioskClient.isConnected()) {
            sendOrderStatusUpdated(orderId, newStatus);
        } else {
            LOGGER.error("KioskNettyClient가 연결되지 않아 주문 상태를 변경할 수 없습니다.");
        }
    }


    @Override
    public Optional<Order> getOrderById(int orderId) {
        return RegistryManager.ORDERS.getById(String.valueOf(orderId));
    }

    private void sendOrderStatusUpdated(int orderId, OrderStatus status) {
        if (kioskClient != null && kioskClient.isConnected()) {
            Optional<Order> oldOrderOpt = getOrderById(orderId);
            if (oldOrderOpt.isEmpty()) {
                LOGGER.error("ID가 {}인 주문을 찾을 수 없어 상태를 변경할 수 없습니다.", orderId);
                return;
            }
            Order oldOrder = oldOrderOpt.get();

            Order updatedOrder = new Order(
                    oldOrder.orderId(),
                    oldOrder.customer(),
                    oldOrder.orderTime(),
                    status,
                    oldOrder.cart()
            );

            OrderStatusChangedC2SPacket packet = new OrderStatusChangedC2SPacket(updatedOrder);

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
}
