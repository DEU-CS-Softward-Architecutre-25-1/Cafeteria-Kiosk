package common.network.packet;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.SynchronizeData;
import common.network.handler.listener.ClientPacketListener;
import common.network.handler.listener.ServerPacketListener;
import common.registry.RegistryManager;
import common.util.JsonHelper;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateDataPacket {
    public record RequestDataC2SPacket(String registryId) implements SidedPacket<ServerPacketListener> {
        public static final Codec<RequestDataC2SPacket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("registryId").forGetter(RequestDataC2SPacket::registryId)
                ).apply(instance, RequestDataC2SPacket::new)
        );

        @Override
        public Side getSide() {
            return Side.SERVER;
        }

        @Override
        public void apply(ServerPacketListener packetListener) {
            packetListener.onRequestData(this);
        }

        @Override
        public String getPacketId() {
            return "request_data_c2s_packet";
        }

        @Override
        public @NotNull Codec<RequestDataC2SPacket> getCodec() {
            return CODEC;
        }
    }

    public record ResponseDataS2CPacket(String registryId, List<SynchronizeData<?>> data) implements SidedPacket<ClientPacketListener> {
        private static final Gson GSON = new Gson();
        public static final Codec<ResponseDataS2CPacket> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(
                string -> {
                    JsonObject json = GSON.fromJson(string, JsonObject.class);
                    String id = JsonHelper.getString(json, "registryId", "unknown");
                    if (id.equalsIgnoreCase("unknown")) {
                        throw new JsonParseException("Unknown registry id: " + id);
                    }
                    var data = RegistryManager.getAsId(id).getCodec().listOf().decode(JsonOps.INSTANCE, json.get("data")).getOrThrow().getFirst();
                    return new ResponseDataS2CPacket(id, (List<SynchronizeData<?>>) data);
                },
                packet -> {
                    JsonObject json = new JsonObject();
                    json.addProperty("registryId", packet.registryId);
                    Codec<SynchronizeData<?>> codec = (Codec<SynchronizeData<?>>) RegistryManager.getAsId(packet.registryId).getCodec();
                    JsonElement arr = codec.listOf().encodeStart(JsonOps.INSTANCE, packet.data).getOrThrow();
                    json.add("data", arr);

                    return json.toString();
                })
        );

        @Override
        public Side getSide() {
            return Side.CLIENT;
        }

        @Override
        public void apply(ClientPacketListener packetListener) {
            packetListener.onReceivedData(this);
        }

        @Override
        public String getPacketId() {
            return "response_data_s2c_packet";
        }

        @Override
        public @NotNull Codec<ResponseDataS2CPacket> getCodec() {
            return CODEC;
        }
    }
}
