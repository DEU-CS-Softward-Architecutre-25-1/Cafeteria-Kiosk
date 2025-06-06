package common.network;

import com.mojang.serialization.Codec;
import common.network.packet.*;
import common.util.CommonUtils;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializable 패킷이 어떤 코드를 사용해야 하는지 등록하는 패킷이다. 일반적으로 {@link Serializable}은 디코딩 시 어떠한 코덱을 사용해야 할 지
 * 알 수 없는 상태이기 때문에 ({@link com.google.gson.JsonObject}이기 때문이다.) root {@link com.google.gson.JsonObject}의 type
 * 값을 이용해 {@link Serializable}의 {@link Codec}을 얻는다. 만약 ID값에 매칭되는 코덱이 존재하지 않을 경우, {@link IllegalArgumentException}이
 * 발생하고, 이로 인해 {@link common.network.handler.SerializableHandler#exceptionCaught(ChannelHandlerContext, Throwable)}에 의해
 * Disconnection 이 발생한다.
 */
public class SerializableManager {
    public static final Marker NETWORK_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker SERIALIZABLE_RECEIVED_MARKER = CommonUtils.makeWithInitialization(MarkerFactory.getMarker("SERIALIZABLE_RECEIVED"), marker -> marker.add(NETWORK_MARKER));
    public static final Marker SERIALIZABLE_SENT_MARKER = CommonUtils.makeWithInitialization(MarkerFactory.getMarker("SERIALIZABLE_SENT"), marker -> marker.add(SERIALIZABLE_RECEIVED_MARKER));

    public static final Map<String, Codec<? extends SidedPacket>> SERIALIZABLE_MAP = new ConcurrentHashMap<>();

    /**
     * 특정 패킷 ID 값에 대한 Codec 을 추가한다.
     * @param key 패킷 ID
     * @param codec 추가할 Codec
     */
    @ApiStatus.Internal
    public static void register(String key, Codec<? extends SidedPacket> codec) {
        synchronized (SERIALIZABLE_MAP) {
            if (SERIALIZABLE_MAP.containsKey(key)) {
                throw new IllegalArgumentException("Key " + key + " is already registered");
            }
            SERIALIZABLE_MAP.put(key, codec);
        }
    }

    public static Optional<Codec<? extends SidedPacket>> getCodec(String key) {
        synchronized (SERIALIZABLE_MAP) {
           return Optional.ofNullable(SERIALIZABLE_MAP.get(key));
        }
    }

    /**
     * 그저 packet을 등록하기 위한 <code>NOP</code> 메소드다.
     */
    public static void initialize() {
        // NOP
    }

    static {
        register("handshake_c2s_info", HandShakeC2SInfo.CODEC);
        register("request_data_c2s_packet", UpdateDataPacket.RequestDataC2SPacket.CODEC);
        register("response_data_s2c_packet", UpdateDataPacket.ResponseDataS2CPacket.CODEC);
        register("hello_s2c_packet", HelloS2CPacket.CODEC);
        register("key_c2s_packet", KeyC2SPacket.CODEC);
        register("encrypt_complete_s2c", EncryptCompleteS2CPacket.CODEC);
        register("data_added_s2c_packet", DataAddedC2SPacket.CODEC);
        register("data_deleted_c2s_packet", DataDeletedC2SPacket.CODEC);
        register("verify_purchase_c2s", VerifyPurchasePackets.VerifyPurchasePacketC2S.CODEC);
    }
}
