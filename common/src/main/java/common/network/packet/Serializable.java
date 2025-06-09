package common.network.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import common.network.SerializableManager;
import org.jetbrains.annotations.NotNull;

/**
 * 실제로 통신되는 패킷의 인터페이스다. 수신받는 입장에서는 실제 구현부 패킷이 무엇인 지를 알 수 없음을 유의하라. <br>
 * {@link Serializable#toJson()} 은 있으나 <code>fromJson()</code> 이 없는 이유는 deserialization을 {@link SerializableManager}에
 * 등록되어있는 코덱이 핸들링 하기 때문이다.
 * @param <T> 실 구현부 타입
 * @see common.network.packet
 */
public interface Serializable<T> {
    String PACKET_ID_PROPERTY = "packetId";
    String DATA_PROPERTY = "data";

    String getPacketId();
    <A extends T> A getValue();
    default JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(PACKET_ID_PROPERTY, getPacketId());
        JsonElement dataValue = getCodec().encodeStart(JsonOps.INSTANCE, getValue()).getOrThrow();
        jsonObject.add(DATA_PROPERTY, dataValue);

        return jsonObject;
    }
    @NotNull
    Codec<? extends T> getCodec();
}
