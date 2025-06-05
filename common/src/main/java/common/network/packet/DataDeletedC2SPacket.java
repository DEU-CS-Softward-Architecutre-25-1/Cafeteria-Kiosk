package common.network.packet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import common.network.handler.listener.ServerPacketListener;
import common.util.JsonHelper;
import org.jetbrains.annotations.NotNull;

public record DataDeletedC2SPacket(String registryId, String dataId) implements SidedPacket<ServerPacketListener> {
    private static final Gson GSON = new Gson();
    public static final Codec<DataDeletedC2SPacket> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(
            string -> {
                JsonObject json = GSON.fromJson(string, JsonObject.class);
                String registryId = JsonHelper.getString(json, "registryId", "unknown");
                if (registryId.equalsIgnoreCase("unknown")) {
                    throw new JsonParseException("Unknown registry id: " + registryId);
                }
                String dataId = JsonHelper.getString(json, "dataId", "");
                if (dataId.isEmpty()) {
                    throw new JsonParseException("Empty data id for deletion");
                }
                return new DataDeletedC2SPacket(registryId, dataId);
            },
            packet -> {
                JsonObject json = new JsonObject();
                json.addProperty("registryId", packet.registryId);
                json.addProperty("dataId", packet.dataId);
                return json.toString();
            })
    );

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

