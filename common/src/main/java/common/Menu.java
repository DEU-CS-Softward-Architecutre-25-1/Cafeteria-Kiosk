package common;

public record Menu(
        int menuId,
        String name,
        double price,
        String categoryId,
        String description,
        String imagePath,
        boolean soldOut
) {
}
