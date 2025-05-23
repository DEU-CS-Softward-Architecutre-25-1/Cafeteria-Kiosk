package common.util;

import java.util.function.Consumer;

public class CommonUtils {
    public static <T> T makeWithInitialization(T object, Consumer<? super T> initializer) {
        initializer.accept(object);
        return object;
    }
}
