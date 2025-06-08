package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.handler.listener.ServerPacketListener;
import org.jetbrains.annotations.NotNull;

public record HandShakeC2SInfo(String id) implements SidedPacket<ServerPacketListener> {
    public static final Codec<HandShakeC2SInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(HandShakeC2SInfo::id)
            ).apply(instance, HandShakeC2SInfo::new)
    );

    @Override
    public Side getSide() {
        return Side.SERVER;
    }

    @Override
    public void apply(ServerPacketListener listener) {
        listener.onHandShake(this);
    }

    @Override
    public String getPacketId() {
        return "handshake_c2s_info";
    }

    @Override
    public @NotNull Codec<HandShakeC2SInfo> getCodec() {
        return CODEC;
    }
}
