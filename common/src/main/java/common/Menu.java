package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record Menu(
        String id,
        String name,
        int price,
        String cateId,
        Path imagePath,
        String description,
        List<OptionGroup> optionGroup
) {
    public static final Codec<Menu> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Menu::id),
            Codec.STRING.fieldOf("name").forGetter(Menu::name),
            Codec.INT.fieldOf("price").forGetter(Menu::price),
            Codec.STRING.fieldOf("cateId").forGetter(Menu::cateId),
            Codec.STRING.xmap(Path::of, Path::toString).fieldOf("imagePath").forGetter(Menu::imagePath),
            Codec.STRING.fieldOf("description").forGetter(Menu::description),
            OptionGroup.CODEC.listOf().fieldOf("optionGroup").forGetter(Menu::optionGroup)
    ).apply(instance, Menu::new));
}