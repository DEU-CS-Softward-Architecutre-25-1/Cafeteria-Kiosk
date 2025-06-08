package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.handler.listener.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

public record EncryptCompleteS2CPacket(long time) implements SidedPacket<ClientPacketListener> {
    public static final Codec<EncryptCompleteS2CPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("time").forGetter(EncryptCompleteS2CPacket::time)
    ).apply(instance, instance.stable(EncryptCompleteS2CPacket::new))
    );

    @Override
    public Side getSide() {
        return Side.CLIENT;
    }

    @Override
    public void apply(ClientPacketListener listener) {
        listener.onEncryptCompleted(this);
    }


    @Override
    public String getPacketId() {
        return "encrypt_complete_s2c";
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }
}
