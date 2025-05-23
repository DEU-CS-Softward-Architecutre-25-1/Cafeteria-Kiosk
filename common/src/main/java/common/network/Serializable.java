package common.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import org.jetbrains.annotations.NotNull;

public interface Serializable<T> {
    String PACKET_ID_PROPERTY = "packetId";
    String DATA_PROPERTY = "data";

    String getPacketId();
    default T fromJson(JsonObject json) {
        if (!json.has("data")) {
            throw new JsonParseException("Can not find data field");
        }
        return getCodec().decode(JsonOps.INSTANCE, json.get("data")).getOrThrow().getFirst();
    }
    <A extends T> A getValue();
    default JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("packetId", getPacketId());
        JsonElement dataValue = getCodec().encodeStart(JsonOps.INSTANCE, getValue()).getOrThrow();
        jsonObject.add("data", dataValue);

        return jsonObject;
    }
    @NotNull
    Codec<? extends T> getCodec();
}
