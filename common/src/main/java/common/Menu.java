package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.SynchronizeData;
import common.registry.RegistryManager;
import common.util.JavaCodecs;

import java.nio.file.Path;
import java.util.List;

public record Menu(String id, String name, int price, Path imagePath,
                   String description, List<OptionGroup> optionGroup, boolean soldOut) implements SynchronizeData<Menu> {

    public final static Codec<Menu> SYNC_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(Menu::id),
                    Codec.STRING.fieldOf("name").forGetter(Menu::name),
                    Codec.INT.fieldOf("price").forGetter(Menu::price),
                    JavaCodecs.PATH.fieldOf("imagePath").forGetter(Menu::imagePath),
                    Codec.STRING.fieldOf("description").forGetter(Menu::description),
                    OptionGroup.CODEC.listOf().fieldOf("optionGroup").forGetter(Menu::optionGroup),
                    Codec.BOOL.fieldOf("soldOut").forGetter(Menu::soldOut)
            ).apply(instance, Menu::new))
    );
    public static final Codec<Menu> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(
            string -> RegistryManager.MENUS
                    .getById(string)
                    .orElseThrow(() -> new IllegalArgumentException("Menu not found")
                    ),
            Menu::id
    ));

    public Menu(String id, String name, int price, Path imagePath, String description, List<OptionGroup> optionGroup) {
        this(id, name, price, imagePath, description, optionGroup, false);
    }

    @Override
    public Codec<Menu> getSyncCodec() {
        return SYNC_CODEC;
    }

    @Override
    public String getRegistryElementId() {
        return this.id;
    }

    public Menu withSoldOut(boolean soldOut) {
        return new Menu(this.id, this.name, this.price, this.imagePath,
                this.description, this.optionGroup, soldOut);
    }
}
