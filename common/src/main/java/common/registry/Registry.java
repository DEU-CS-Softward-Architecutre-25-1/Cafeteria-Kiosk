package common.registry;

import com.mojang.serialization.Codec;
import common.network.SynchronizeData;
import common.util.IndexIterable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * 동기화 될 요소들이 저장되는 콘테이너의 인터페이스이다. 전체 레지스트리는 {@link RegistryManager}를 통해 확인할 수 있다.
 * @param <T> 저장될 데이터 타입
 */
public interface Registry<T extends SynchronizeData<?>> extends IndexIterable<T> {
    String getRegistryId();
    Optional<T> getById(String id);
    boolean isFrozen();
    void freeze();
    void unfreeze();
    T add(String id, SynchronizeData<?> entry);
    void addAll(List<SynchronizeData<?>> data);
    @NotNull
    Codec<T> getCodec();
    List<T> getAll();
    void clear();
    /**
     * 왜 ClassType 을 요구하는지 의아할 수 있다. 이는 add 에서 <code>Serializable</code>을 안전하게 넣기 위함이다. 클라이언트에서 이러한
     * 데이터 타입을 받았을 때 상술한 데이터타입으로 들어오는데, 이 때 알맞지 않은 데이터 타입이 들어올 수도 있다. 이를 위해 ClassType을 지정하여
     * {@link Registry#add(String, SynchronizeData)}에서 이를 확인함으로서 type-safe 하게 데이터를 저장 할 수 있다.
     * @return 저장되는 데이터의 클래스타입
     */
    Class<T> getClazz();
}
