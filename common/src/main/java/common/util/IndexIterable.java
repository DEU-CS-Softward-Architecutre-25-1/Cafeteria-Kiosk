package common.util;

import common.network.Serializable;
import org.jetbrains.annotations.Nullable;

public interface IndexIterable<T> extends Iterable<T> {
    int ABSENT_LOW_INDEX = -1;
    int getRawId(T var1);

    @Nullable
    T get(int index);

    default T getOrThrow(int index) {
        T object = this.get(index);
        if (object == null) {
            throw new IllegalArgumentException("No value with id " + index);
        }
        return object;
    }

    default int getRawIdOrThrow(T value) {
        int i = this.getRawId(value);
        if (i == -1) {
            throw new IllegalArgumentException("Can't find id for '" + String.valueOf(value) + "' in map " + String.valueOf(this));
        }
        return i;
    }
    int size();
}
