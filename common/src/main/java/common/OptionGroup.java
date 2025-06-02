package common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.TestOnly;

import java.util.List;
import java.util.Map;

public record OptionGroup(
        String id,
        String name,
        boolean required,
        List<Option> options
) {

    public static final Codec<OptionGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(OptionGroup::id),
            Codec.STRING.fieldOf("name").forGetter(OptionGroup::name),
            Codec.BOOL.fieldOf("required").forGetter(OptionGroup::required),
            Option.CODEC.listOf().fieldOf("options").forGetter(OptionGroup::options)
    ).apply(instance, OptionGroup::new));

    @TestOnly
    public static List<OptionGroup> loadOptionGroups(String menuId) {
        Map<String, List<OptionGroup>> mockOptionGroups = Map.of(
                "menu001", List.of(
                        new OptionGroup(
                                "temp", "온도", true,
                                List.of(
                                        new Option("temp_hot", "뜨겁게", 0),
                                        new Option("temp_cold", "차갑게", 0)
                                )
                        ),
                        new OptionGroup(
                                "ice", "얼음 양", false,
                                List.of(
                                        new Option("ice_none", "없음", 0),
                                        new Option("ice_regular", "기본", 0),
                                        new Option("ice_more", "많이", 0)
                                )
                        )
                ),
                "menu002", List.of(
                        new OptionGroup(
                                "temp", "온도", true,
                                List.of(
                                        new Option("temp_hot", "뜨겁게", 0),
                                        new Option("temp_warm", "미지근하게", 0),
                                        new Option("temp_cold", "차갑게", 0)
                                )
                        ),
                        new OptionGroup(
                                "shot", "샷 추가", false,
                                List.of(
                                        new Option("shot_default", "기본", 0),
                                        new Option("shot_1", "1샷 추가", 500),
                                        new Option("shot_2", "2샷 추가", 1000)
                                )
                        )
                ),
                "menu003", List.of(
                        new OptionGroup(
                                "syrup", "시럽 선택", false,
                                List.of(
                                        new Option("syrup_none", "없음", 0),
                                        new Option("syrup_vanilla", "바닐라 시럽", 300),
                                        new Option("syrup_hazelnut", "헤이즐넛 시럽", 300)
                                )
                        )
                )
        );

        return mockOptionGroups.getOrDefault(menuId, List.of());
    }
}
