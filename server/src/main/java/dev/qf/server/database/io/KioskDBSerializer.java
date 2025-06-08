package dev.qf.server.database.io;

import common.Category;
import common.Menu;
import common.Option;
import common.OptionGroup;
import common.network.SynchronizeData;
import common.registry.RegistryManager;
import common.util.KioskLoggerFactory;
import it.unimi.dsi.fastutil.Function;
import me.mrnavastar.sqlib.api.DataContainer;
import me.mrnavastar.sqlib.api.types.JavaTypes;
import me.mrnavastar.sqlib.api.types.SQLibType;
import me.mrnavastar.sqlib.impl.SQLPrimitive;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class KioskDBSerializer {
    private static final Logger logger = KioskLoggerFactory.getLogger();
    private static final Map<Class<?>, BiConsumer<DataContainer, SynchronizeData<?>>> SERIALIZER = new HashMap<>();
    private static final Map<Class<?>, Function<DataContainer, SynchronizeData<?>>> DESERIALIZER = new HashMap<>();

    private static final SQLibType<Path> PATH = new SQLibType<Path>(SQLPrimitive.STRING, Path::toString, Path::of);

    private static <T extends SynchronizeData<?>> void registerSerializer(Class<T> clazz, BiConsumer<DataContainer, T> consumer) {
        SERIALIZER.put((Class<?>) clazz, (BiConsumer<DataContainer, SynchronizeData<?>>) consumer);
    }

    public static void serialize(DataContainer container, SynchronizeData<?> serializable) {
        if (SERIALIZER.containsKey(serializable.getClass())) {
            SERIALIZER.get(serializable.getClass()).accept(container, serializable);
        } else {
            logger.warn("Can not found serializer for " + serializable.getClass());
        }
    }

    private static void registerDeserializer(Class<?> clazz, Function<DataContainer, SynchronizeData<?>> deserializer) {
        DESERIALIZER.put(clazz, deserializer);
    }

    public static <R extends SynchronizeData<?>> R deserialize(Class<R> clazz, DataContainer container) {
        if (DESERIALIZER.containsKey(clazz)) {
            return (R) DESERIALIZER.get(clazz).apply(container);
        } else {
            throw new RuntimeException("Can not found deserializer for " + clazz);
        }
    }

    static {
        registerSerializer(Option.class, (container, option) -> {
            container.put(JavaTypes.STRING, "id", option.id());
            container.put(JavaTypes.STRING, "name", option.name());
            container.put(JavaTypes.INT, "extra_cost", option.extraCost());
        });

        registerDeserializer(Option.class, (dc) -> {
            DataContainer dataContainer = (DataContainer) dc;
            String id = dataContainer.get(JavaTypes.STRING, "id").orElseThrow();
            String name = dataContainer.get(JavaTypes.STRING, "name").orElseThrow();
            int extraCost = dataContainer.get(JavaTypes.INT, "extra_cost").orElseThrow();

            return new Option(id, name, extraCost);
        });

        registerSerializer(OptionGroup.class, (container, optionGroup) -> {
            container.put(JavaTypes.STRING, "id", optionGroup.id());
            container.put(JavaTypes.STRING, "name", optionGroup.name());
            container.put(JavaTypes.BOOL, "required", optionGroup.required());
            String[] options = optionGroup.options().stream().map(Option::id).toArray(String[]::new);
            container.put(JavaTypes.STRING, "options", Arrays.toString(options).replace("[", "").replace("]", ""));
        });

        registerDeserializer(OptionGroup.class, (dc) -> {
            DataContainer dataContainer = (DataContainer) dc;
            String id = dataContainer.get(JavaTypes.STRING, "id").orElseThrow();
            String name = dataContainer.get(JavaTypes.STRING, "name").orElseThrow();
            boolean required = dataContainer.get(JavaTypes.BOOL, "required").orElse(false);
            String[] options = dataContainer.get(JavaTypes.STRING, "options").orElseThrow().split(",");
            Option[] optionArray = Arrays.stream(options)
                    .map(optionId -> RegistryManager.OPTIONS.getById(optionId)
                            .orElseThrow(() -> new IllegalStateException("Option not found : " + optionId)))
                    .toArray(Option[]::new);

            return new OptionGroup(id, name, required, Arrays.stream(optionArray).toList());
        });

        registerSerializer(Menu.class, (container, menu) -> {
            container.put(JavaTypes.STRING, "id", menu.id());
            container.put(JavaTypes.STRING, "name", menu.id());
            container.put(JavaTypes.INT, "price", menu.price());
            container.put(PATH, "image_path", menu.imagePath());
            container.put(JavaTypes.STRING, "description", menu.description());
            container.put(JavaTypes.STRING, "option_groups", Arrays
                    .toString(menu.optionGroup().stream().map(OptionGroup::id).toArray(String[]::new))
                        .replace("[", "")
                        .replace("]", "")
            );
        });

        registerDeserializer(Menu.class, (dc) -> {
            DataContainer dataContainer = (DataContainer) dc;
            String id = dataContainer.get(JavaTypes.STRING, "id").orElseThrow();
            String name = dataContainer.get(JavaTypes.STRING, "name").orElseThrow();
            int price = dataContainer.get(JavaTypes.INT, "price").orElseThrow();
            Path imagePath = dataContainer.get(PATH, "image_path").orElseThrow();
            String description = dataContainer.get(JavaTypes.STRING, "description").orElseThrow();
            String[] optionGroups = dataContainer.get(JavaTypes.STRING, "option_groups").orElseThrow().split(",");
            List<OptionGroup> optionGroupList = Arrays
                    .stream(optionGroups)
                    .map(optionId -> RegistryManager.OPTION_GROUPS
                        .getById(optionId)
                        .orElseThrow(() -> new IllegalStateException("OptionGroup not found")))
                    .toList();

            return new Menu(id, name, price, imagePath, description, optionGroupList);
        });

        registerSerializer(Category.class, (container, category) -> {
            container.put(JavaTypes.STRING, "id", category.cateId());
            container.put(JavaTypes.STRING, "name", category.cateName());
            String[] menuIds = category.menus().stream().map(Menu::id).toArray(String[]::new);
            container.put(JavaTypes.STRING, "menus", Arrays.toString(menuIds).replace("[", "").replace("]", ""));
        });

        registerDeserializer(Category.class, (dc) -> {
            DataContainer dataContainer = (DataContainer) dc;
            String id = dataContainer.get(JavaTypes.STRING, "id").orElseThrow();
            String name = dataContainer.get(JavaTypes.STRING, "name").orElseThrow();
            String[] menuIds = dataContainer.get(JavaTypes.STRING, "menus").orElseThrow().split(",");
            List<Menu> menuList = Arrays
                    .stream(menuIds)
                    .map(menuId -> RegistryManager.MENUS.getById(menuId)
                            .orElseThrow(() -> new IllegalStateException("Menu not found")))
                    .toList();

            return new Category(id, name, menuList);
        });
    }
}
