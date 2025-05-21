package common;

import java.util.List;

public record Menu(
        String id,
        String name,
        int price,
        String cateId,
        String imagePath,
        String description,
        List<OptionGroup> optionGroup
) {}
