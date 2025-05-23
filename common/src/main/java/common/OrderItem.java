package common;

public record OrderItem(String name, int quantity, String option) {
    public static final OrderItem EMPTY = new OrderItem("알 수 없음", 0, "알 수 없음");
}
