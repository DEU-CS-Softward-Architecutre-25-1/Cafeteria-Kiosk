package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.util.JavaCodecs;

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
) implements SynchronizeData<Order> {
    public static final Codec<Order> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("order_id").forGetter(Order::orderId),
                    Codec.STRING.fieldOf("customer").forGetter(Order::customer),
                    JavaCodecs.LOCAL_DATE_TIME.fieldOf("order_time").forGetter(Order::orderTime),
                    OrderStatus.CODEC.fieldOf("status").forGetter(Order::status),
                    Cart.CODEC.fieldOf("cart").forGetter(Order::cart)
            ).apply(instance, Order::new))
    );
    public static final Order EMPTY = new Order(-1, "UNKNOWN", LocalDateTime.MIN, OrderStatus.UNKNOWN, Cart.EMPTY);

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
        return String.valueOf(this.orderId);
    }
}
