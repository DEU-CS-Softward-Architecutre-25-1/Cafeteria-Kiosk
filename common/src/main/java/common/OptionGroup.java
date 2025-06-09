package common;

import common.network.SynchronizeData;
import common.registry.RegistryManager;
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
) implements SynchronizeData<OptionGroup> {
    public static final Codec<OptionGroup> SYNC_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(OptionGroup::id),
                    Codec.STRING.fieldOf("name").forGetter(OptionGroup::name),
                    Codec.BOOL.fieldOf("required").forGetter(OptionGroup::required),
                    Option.CODEC.listOf().fieldOf("options").forGetter(OptionGroup::options)
            ).apply(instance, OptionGroup::new)
    );

    public static final Codec<OptionGroup> CODEC = Codec.lazyInitialized(() -> Codec.STRING.xmap(
            s -> RegistryManager.OPTION_GROUPS.getById(s).orElseThrow(() -> new IllegalArgumentException("OptionGroup not found"))
            , OptionGroup::id
            ));

    @TestOnly
    //임시로 테스트를 위해 넣은 예시
    public static List<OptionGroup> loadOptionGroups(String menuId) {
        // 메뉴별 옵션 그룹 정의
        Map<String, List<OptionGroup>> mockOptionGroups = Map.of(
                "menu001", List.of( // 아메리카노
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
                "menu002", List.of( // 카페라떼
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
                "menu003", List.of( // 바닐라라떼
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

    @Override
    public Codec<OptionGroup> getSyncCodec() {
        return SYNC_CODEC;
    }

    @Override
    public String getRegistryElementId() {
        return this.id;
    }
}
