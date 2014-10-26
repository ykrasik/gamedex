package com.github.ykrasik.indexter.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class OptionalUtils {
    private OptionalUtils() {
    }

    public static <T> List<T> toList(Optional<T> optional) {
        if (optional.isPresent()) {
            return Collections.singletonList(optional.get());
        } else {
            return Collections.emptyList();
        }
    }

    public static <T, A> List<A> mapToList(Optional<T> optional, Function<T, A> f) {
        if (optional.isPresent()) {
            return Collections.singletonList(f.apply(optional.get()));
        } else {
            return Collections.emptyList();
        }
    }

    public static <T, A> List<A> flatMapToList(Optional<T> optional, Function<T, List<A>> f) {
        if (optional.isPresent()) {
            return f.apply(optional.get());
        } else {
            return Collections.emptyList();
        }
    }

    public static <T, K, V> Map<K, V> flatMapToMap(Optional<T> optional, Function<T, Map<K, V>> f) {
        if (optional.isPresent()) {
            return f.apply(optional.get());
        } else {
            return Collections.emptyMap();
        }
    }
}
