package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.Order;
import common.network.handler.listener.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

public record OrderUpdatedS2CPacket(Order order) implements SidedPacket<ClientPacketListener> {
    public static final Codec<OrderUpdatedS2CPacket> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance ->
            instance.group(
                    Order.CODEC.fieldOf("order").forGetter(OrderUpdatedS2CPacket::order)
            ).apply(instance, instance.stable(OrderUpdatedS2CPacket::new))
    ));

    @Override
    public Side getSide() {
        return Side.CLIENT;
    }

    @Override
    public void apply(ClientPacketListener listener) {
        listener.onOrderStatusChanged(this);
    }

    @Override
    public String getPacketId() {
        return "order_updated_s2c";
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }
}
