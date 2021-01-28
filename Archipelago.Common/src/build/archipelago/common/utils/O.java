package build.archipelago.common.utils;

import java.util.Optional;
import java.util.function.Function;

public final class O {
    public static <T> boolean isPresent(Optional<T> optional) {
        return optional != null && optional.isPresent();
    }

    public static <T, R> R getOrNull(Optional<T> optional, Function<T, R> onGet) {
        return getOrDefault(optional, null, onGet);
    }

    public static <T, R> R getOrDefault(Optional<T> optional, R defaultValue, Function<T, R> onGet) {
        if (!isPresent(optional)) {
            return defaultValue;
        }
        return onGet.apply(optional.get());
    }

    public static <T> T getOrNull(Optional<T> optional) {
        return getOrDefault(optional, null, (v) -> v);
    }

    public static <T> T getOrDefault(Optional<T> optional, T defaultValue) {
        return getOrDefault(optional, defaultValue, (v) -> v);
    }
}
