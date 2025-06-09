package common.network.packet;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import common.network.SynchronizeData;
import common.network.handler.listener.ServerPacketListener;
import common.registry.RegistryManager;
import common.util.JsonHelper;
import org.jetbrains.annotations.NotNull;

public record DataAddedC2SPacket(String registryId, SynchronizeData<?> data) implements SidedPacket<ServerPacketListener> {
    private static final Gson GSON = new Gson();
    public static final Codec<DataAddedC2SPacket> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(
            string -> {
                JsonObject json = GSON.fromJson(string, JsonObject.class);
                String id = JsonHelper.getString(json, "registryId", "unknown");
                if (id.equalsIgnoreCase("unknown")) {
                    throw new JsonParseException("Unknown registry id: " + id);
                }
                var data = RegistryManager.getAsId(id).getCodec().decode(JsonOps.INSTANCE, json.get("data")).getOrThrow().getFirst();
                return new DataAddedC2SPacket(id, data);
            },
            packet -> {
                JsonObject json = new JsonObject();
                json.addProperty("registryId", packet.registryId);
                Codec<SynchronizeData<?>> codec = (Codec<SynchronizeData<?>>) RegistryManager.getAsId(packet.registryId).getCodec();
                JsonElement ele = codec.encodeStart(JsonOps.INSTANCE, packet.data).getOrThrow();
                json.add("data", ele);

                return json.toString();
            })
    );

    @Override
    public Side getSide() {
        return Side.SERVER;
    }

    @Override
    public void apply(ServerPacketListener listener) {
        listener.onUpdateReceived(this);
    }

    @Override
    public String getPacketId() {
        return "data_added_s2c_packet";
    }

    @Override
    public @NotNull Codec<? extends SidedPacket> getCodec() {
        return CODEC;
    }
}
