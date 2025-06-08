package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import common.network.SynchronizeData;
import common.registry.RegistryManager;

import java.util.List;
import java.util.Set;


public record Category(String cateId, String cateName, List<Menu> menus) implements SynchronizeData<Category> {
    public static final Codec<Category> SYNC_CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("cateId").forGetter(Category::cateId),
                    Codec.STRING.fieldOf("cateName").forGetter(Category::cateName),
                    Menu.CODEC.listOf().fieldOf("categories").forGetter(Category::menus)
            ).apply(instance, Category::new)
    ));

    @Override
    public Codec<Category> getSyncCodec() {
        return SYNC_CODEC;
    }

    @Override
    public String getRegistryElementId() {
        return this.cateId;
    }

    public static Category getMenuById(String menuId) {
        Menu menu = RegistryManager.MENUS.getById(menuId).orElseThrow(() -> new IllegalArgumentException("Menu not found"));
        for (var category : RegistryManager.CATEGORIES) {
            if (category.menus.contains(menu)) {
                return category;
            }
        }

        return null;
    }
}
