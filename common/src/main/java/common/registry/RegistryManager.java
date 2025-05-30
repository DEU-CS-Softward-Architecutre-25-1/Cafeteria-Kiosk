package common.registry;

import com.google.common.collect.ImmutableList;
import common.Category;
import common.Menu;
import common.Option;
import common.OptionGroup;
import common.network.SynchronizeData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Serializable 순서
 * 1. Options
 * 2. OptioGroups
 * 3. MENUS
 * 4. CATEGORIES
 */
public class RegistryManager {
    private static final HashMap<String, Registry<?>> REGISTRY_MAP = new HashMap<>();
    private static final List<Registry<?>> ENTRIES = new ArrayList<>();
    public static Registry<?> getAsId(String id) {
        return REGISTRY_MAP.get(id);
    }

    public static final Registry<Option> OPTIONS = new SimpleRegistry<>("options", Option.SYNC_CODEC, Option.class);
    public static final Registry<OptionGroup> OPTION_GROUPS = new SimpleRegistry<>("option_groups", OptionGroup.SYNC_CODEC, OptionGroup.class);
    public static final Registry<Menu> MENUS = new SimpleRegistry<>("menus", Menu.SYNC_CODEC, Menu.class);
    public static final Registry<Category> CATEGORIES = new SimpleRegistry<>("categories", Category.SYNC_CODEC, Category.class);

    private static void addRegistry(Registry<?> registry) {
        REGISTRY_MAP.put(registry.getRegistryId(), registry);
        ENTRIES.add(registry);
    }

    public static List<Registry<?>> entries() {
        return ImmutableList.copyOf(ENTRIES);
    }

    @Nullable
    public static <T extends SynchronizeData<?>> Registry<T> getByClassType(Class<T> clazz) {
        for (Registry<?> entry : entries()) {
            if (entry.getClazz() == clazz) {
                return (Registry<T>) entry;
            }
        }
        return null;
    }

    static {
        addRegistry(OPTIONS);
        addRegistry(OPTION_GROUPS);
        addRegistry(MENUS);
        addRegistry(CATEGORIES);
    }
}
