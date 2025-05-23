package common.network;

import com.mojang.serialization.Codec;
import common.network.packet.HandShakeC2SInfo;
import common.network.packet.SidedPacket;
import common.util.CommonUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SerializableManager {
    public static final Marker NETWORK_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker SERIALIZABLE_RECEIVED_MARKER = CommonUtils.makeWithInitialization(MarkerFactory.getMarker("SERIALIZABLE_RECEIVED"), marker -> marker.add(NETWORK_MARKER));
    public static final Marker SERIALIZABLE_SENT_MARKER = CommonUtils.makeWithInitialization(MarkerFactory.getMarker("SERIALIZABLE_SENT"), marker -> marker.add(SERIALIZABLE_RECEIVED_MARKER));

    public static final Map<String, Codec<? extends SidedPacket>> SERIALIZABLE_MAP = new ConcurrentHashMap<>();

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

    public static void initialize() {

    }

    static {
        register("handshake_c2s_info", HandShakeC2SInfo.CODEC);
    }
}
