package com.github.ykrasik.indexter.util;

import java.util.*;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class ListUtils {
    private ListUtils() {
    }

    public static <T, A> List<A> map(Collection<T> collection, Function<T, A> f) {
        final List<A> retList = new ArrayList<>(collection.size());
        for (T element : collection) {
            retList.add(f.apply(element));
        }
        return retList;
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
