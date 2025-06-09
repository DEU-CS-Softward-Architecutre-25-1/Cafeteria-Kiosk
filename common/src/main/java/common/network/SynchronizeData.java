package common.network;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import common.network.packet.DataAddedC2SPacket;
import common.network.packet.SidedPacket;
import common.registry.RegistryManager;
import common.util.Container;

/**
 * 동기화 될 수 있는 자료형들에 대한 인터페이스이다. 해당 인터페이스의 구현체들은 {@link common.network.packet.UpdateDataPacket.ResponseDataS2CPacket}
 * 를 통해 서버에서 클라이언트로 전송 될 수 있다. 반대의 경우는 허용되지 않으나, 특정 요소가 추가되었을 때는 클라이언트에서 서버로 전달될 수 있다.
 * @param <T> DataType
 * @see common.Menu
 * @see common.Option
 * @see common.OptionGroup
 * @see common.Category
 */
public interface SynchronizeData<T extends SynchronizeData<?>> {
    Codec<T> getSyncCodec();
    default JsonElement toJson() {
       return getSyncCodec().encodeStart(JsonOps.INSTANCE, (T) this).getOrThrow();
    }
    String getRegistryElementId();
    default void sendUpdated() {
        Connection connection = Container.get(Connection.class);
        if (connection.getSide() != SidedPacket.Side.CLIENT) {
            throw new IllegalStateException("Cannot send update packet from server");
        }
        connection.sendSerializable("server", new DataAddedC2SPacket(RegistryManager.getByClassType(this.getClass()).getRegistryId(), this));
    }
}
