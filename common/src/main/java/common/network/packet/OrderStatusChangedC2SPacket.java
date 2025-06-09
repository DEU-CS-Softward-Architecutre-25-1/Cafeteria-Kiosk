package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.Order;
import common.network.handler.listener.ServerPacketListener;
import org.jetbrains.annotations.NotNull;

public record OrderStatusChangedC2SPacket(Order order) implements SidedPacket<ServerPacketListener> {
    public static final Codec<OrderStatusChangedC2SPacket> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance ->
            instance.group(
                    Order.CODEC.fieldOf("order").forGetter(OrderStatusChangedC2SPacket::order)
            ).apply(instance, OrderStatusChangedC2SPacket::new))
    );
    @Override
    public Side getSide() {
        return Side.SERVER;
    }

    @Override
    public void apply(ServerPacketListener listener) {
        listener.onOrderStatusChanged(this);
    }

    @Override
    public String getPacketId() {
        return "order_status_changed_c2s";
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }
}
