package com.github.ykrasik.indexter.optional;

import com.github.ykrasik.indexter.exception.FunctionThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class Optionals {
    private Optionals() {
    }

    public static <T> String toString(Optional<T> optional, String absent) {
        return optional.map(Object::toString).orElse(absent);
    }

    public static <T> String toStringOrUnavailable(Optional<T> optional) {
        return toString(optional, "Unavailable");
    }

    public static <T> Optional<T> or(Optional<T> that, Optional<T> other) {
        return that.isPresent() ? that : other;
    }

    public static <T> List<T> toList(Optional<T> optional) {
        if (optional.isPresent()) {
            return Collections.singletonList(optional.get());
        } else {
            return Collections.emptyList();
        }
    }

    public static <T> Optional<T> fromList(List<T> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            if (list.size() > 1) {
                throw new IllegalArgumentException("List is too large to be converted to an optional: " + list);
            }
            return Optional.of(list.get(0));
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

    public static <T, R> Optional<R> map(Optional<T> optional, FunctionThrows<T, R> function) {
        if (optional.isPresent()) {
            try {
                return Optional.of(function.apply(optional.get()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return Optional.empty();
        }
    }

    public static <T, R> Optional<R> flatMap(Optional<T> optional, FunctionThrows<T, Optional<R>> function) {
        if (optional.isPresent()) {
            try {
                return function.apply(optional.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return Optional.empty();
        }
    }
}
