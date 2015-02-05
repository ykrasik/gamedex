package com.github.ykrasik.indexter.util;

import com.github.ykrasik.indexter.util.exception.FunctionThrows;

import java.util.*;
import java.util.function.Function;

import static com.github.ykrasik.indexter.util.exception.ExceptionWrappers.rethrow;

/**
 * @author Yevgeny Krasik
 */
public final class ListUtils {
    private ListUtils() {
    }

    public static <T, A> List<A> map(Collection<T> collection, FunctionThrows<T, A> f) {
        return rethrow(() -> {
            final List<A> retList = new ArrayList<>(collection.size());
            for (T element : collection) {
                retList.add(f.apply(element));
            }
            return retList;
        });
    }

    public static <T, A> List<A> mapToOption(Collection<T> collection, FunctionThrows<T, Optional<A>> f) {
        return rethrow(() -> {
            final List<A> retList = new ArrayList<>(collection.size());
            for (T element : collection) {
                final Optional<A> option = f.apply(element);
                option.ifPresent(retList::add);
            }
            return retList;
        });
    }

    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> idFunction) {
        final Map<K, T> map = new HashMap<>(collection.size());
        for (T element : collection) {
            final K id = idFunction.apply(element);
            if (map.containsKey(id)) {
                throw new IllegalArgumentException("Map already contains an entry for: " + id);
            }
            map.put(id, element);
        }
        return map;
    }
}
