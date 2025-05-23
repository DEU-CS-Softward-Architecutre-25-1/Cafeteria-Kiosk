package common.registry;

import com.mojang.serialization.Codec;
import common.network.SynchronizeData;
import common.util.IndexIterable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Registry<T extends SynchronizeData<?>> extends IndexIterable<T> {
    String getRegistryId();
    Optional<T> getById(String id);
    boolean isFrozen();
    void freeze();
    void unfreeze();
    T add(String id, T entry);
    @NotNull
    Codec<T> getCodec();
}
