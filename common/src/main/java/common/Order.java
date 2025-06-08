package common;

import java.time.LocalDateTime;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.SynchronizeData;
import common.network.packet.Serializable;
import common.util.JavaCodecs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import common.util.KioskLoggerFactory;
import org.slf4j.Logger;

public record Order(
        int orderId,
        String customer,
        LocalDateTime orderTime,
        OrderStatus status,
        Cart cart
) implements SynchronizeData<Order>, Serializable<Order> {

    private static final Logger LOGGER = KioskLoggerFactory.getLogger();

    public static final Order EMPTY = new Order(-1, "UNKNOWN", LocalDateTime.MIN, OrderStatus.UNKNOWN, Cart.EMPTY);

    public final static Codec<Order> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("orderId").forGetter(Order::orderId),
                    Codec.STRING.fieldOf("customer").forGetter(Order::customer),
                    JavaCodecs.LOCAL_DATE_TIME.fieldOf("orderTime").forGetter(Order::orderTime),
                    Codec.STRING.xmap(OrderStatus::valueOf, OrderStatus::name).fieldOf("status").forGetter(Order::status),
                    Cart.CODEC.fieldOf("cart").forGetter(Order::cart)
            ).apply(instance, (orderId, customer, orderTime, status, cart) -> {
                LOGGER.info("DEBUG: Order Codec: Successfully decoded Order ID: {}, Customer: {}", orderId, customer);
                return new Order(orderId, customer, orderTime, status, cart);
            })
    );

    public Order(int orderId, String customer, LocalDateTime orderTime, OrderStatus status) {
        this(orderId, customer, orderTime, status, new Cart());
    }

    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.orderId, this.customer, this.orderTime, newStatus, this.cart);
    }

    @Override
    public Codec<Order> getSyncCodec() {
        return CODEC;
    }

    @Override
    public String getRegistryElementId() {
        return "order:" + orderId();
    }

    @Override
    public String getPacketId() {
        return "order_data_element";
    }

    @Override
    public Order getValue() {
        return this;
    }

    @Override
    public Codec<Order> getCodec() {
        return CODEC;
    }

    @Override
    public JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(Serializable.PACKET_ID_PROPERTY, getPacketId());
        JsonElement dataValue = getCodec().encodeStart(JsonOps.INSTANCE, getValue()).getOrThrow();
        jsonObject.add(Serializable.DATA_PROPERTY, dataValue);
        return jsonObject;
    }
}