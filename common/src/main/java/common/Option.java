package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.SynchronizeData;
import common.registry.RegistryManager;

public record Option(String id, String name, int extraCost) implements SynchronizeData<Option> {
    public static final Codec<Option> SYNC_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(Option::id),
                    Codec.STRING.fieldOf("name").forGetter(Option::name),
                    Codec.INT.fieldOf("extra_cost").forGetter(Option::extraCost)
            ).apply(instance, Option::new)
    );

    public static final Codec<Option> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(
            string -> RegistryManager.OPTIONS
                    .getById(string)
                    .orElseThrow(() -> new IllegalArgumentException("Option not found")
                    )
            , Option::id)
    );

    @Override
    public Codec<Option> getSyncCodec() {
        return SYNC_CODEC;
    }

    @Override
    public String getRegistryElementId() {
        return this.id;
    }
}
