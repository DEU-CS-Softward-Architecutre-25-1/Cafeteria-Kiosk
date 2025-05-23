package common.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 사실 좋은 구현 방법이 아니라는 걸 알지만 뭐...
 */
public class Container {
    private static final Map<Class<?>, Object> CLASS_MAP = new HashMap<>();

    public static <T> void put(Class<T> clazz, T instance) {
        CLASS_MAP.put(clazz, instance);
    }

    public static <T> T get(Class<T> clazz) {
        return (T) CLASS_MAP.get(clazz);
    }
}
