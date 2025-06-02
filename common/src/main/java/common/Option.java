package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Option(
        String id,
        String name,
        int extraCost
) {
    public static final Codec<Option> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Option::id),
            Codec.STRING.fieldOf("name").forGetter(Option::name),
            Codec.INT.fieldOf("extraCost").forGetter(Option::extraCost)
    ).apply(instance, Option::new));
}
