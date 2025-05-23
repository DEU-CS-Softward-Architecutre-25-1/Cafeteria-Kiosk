package common.network;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

public interface SynchronizeData<T extends SynchronizeData<?>> {
    Codec<T> getCodec();
    default JsonElement toJson() {
       return getCodec().encodeStart(JsonOps.INSTANCE, (T) this).getOrThrow();
    }
    default T fromJson(JsonElement json) {
        return getCodec().decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
    }
}
