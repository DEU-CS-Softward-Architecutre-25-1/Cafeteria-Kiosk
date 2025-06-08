package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.Cart;
import common.network.handler.listener.ClientPacketListener;
import common.network.handler.listener.ServerPacketListener;
import org.jetbrains.annotations.NotNull;

public class VerifyPurchasePackets {
    public record VerifyPurchasePacketC2S(long time, Cart cart) implements SidedPacket<ServerPacketListener> {
        public static final Codec<VerifyPurchasePacketC2S> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.LONG.fieldOf("time").forGetter(VerifyPurchasePacketC2S::time),
                        Cart.CODEC.fieldOf("cart").forGetter(VerifyPurchasePacketC2S::cart)
                ).apply(instance, instance.stable(VerifyPurchasePacketC2S::new))
        );

        @Override
        public Side getSide() {
            return Side.SERVER;
        }

        @Override
        public void apply(ServerPacketListener listener) {
            listener.onRequestVerify(this);
        }

        @Override
        public String getPacketId() {
            return "verify_purchase_c2s";
        }

        @Override
        public @NotNull Codec<? extends SidedPacket> getCodec() {
            return CODEC;
        }
    }

    public record VerifyPurchaseResultS2CPacket(boolean result) implements SidedPacket<ClientPacketListener> {
        public static final Codec<VerifyPurchaseResultS2CPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("result").forGetter(VerifyPurchaseResultS2CPacket::result)
        ).apply(instance, instance.stable(VerifyPurchaseResultS2CPacket::new)));

        @Override
        public Side getSide() {
            return Side.CLIENT;
        }

        @Override
        public void apply(ClientPacketListener listener) {
            listener.onVerifyPurchaseResult(this);
        }

        @Override
        public String getPacketId() {
            return "verify_purchase_result_s2c";
        }

        @Override
        public @NotNull Codec<? extends SidedPacket> getCodec() {
            return CODEC;
        }
    }
}
