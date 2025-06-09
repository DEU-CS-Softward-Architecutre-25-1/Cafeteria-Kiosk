package common.network.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.handler.listener.ServerPacketListener;
import org.jetbrains.annotations.NotNull;

public record DataDeletedC2SPacket(String registryId, String dataId) implements SidedPacket<ServerPacketListener> {
    public static final Codec<DataDeletedC2SPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
       Codec.STRING.fieldOf("registry_id").forGetter(DataDeletedC2SPacket::registryId),
       Codec.STRING.fieldOf("data_id").forGetter(DataDeletedC2SPacket::dataId)
    ).apply(instance, DataDeletedC2SPacket::new));

    @Override
    public Side getSide() {
        return Side.SERVER;
    }

    @Override
    public void apply(ServerPacketListener listener) {
        listener.onDeleteReceived(this);
    }

    @Override
    public String getPacketId() {
        return "data_deleted_c2s_packet"; // SerializableManager에 등록된 키와 정확히 일치
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }
}

