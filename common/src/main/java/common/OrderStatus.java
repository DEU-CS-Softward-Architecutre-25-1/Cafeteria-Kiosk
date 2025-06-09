package common;

import com.mojang.serialization.Codec;
import common.util.StringIdentifiable;

public enum OrderStatus implements StringIdentifiable {
    PENDING,
    ACCEPTED,
    CANCELED,
    UNKNOWN;

    public static Codec<OrderStatus> CODEC = StringIdentifiable.createCodec(OrderStatus::values);

    @Override
    public String asString() {
        return this.name().toLowerCase();
    }
}
