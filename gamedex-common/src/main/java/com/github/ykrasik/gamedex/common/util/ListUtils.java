package com.github.ykrasik.gamedex.common.util;

import com.github.ykrasik.opt.util.FunctionThrows;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Yevgeny Krasik
 */
public final class ListUtils {
    private ListUtils() {
    }

    public static <T, A> List<A> map(List<T> collection, Function<T, A> f) {
        return collection.stream().map(f).collect(Collectors.toList());
    }

    @SneakyThrows
    public static <T, A> List<A> mapX(List<T> collection, FunctionThrows<T, A> f) {
        final List<A> retList = new ArrayList<>(collection.size());
        for (T element : collection) {
            retList.add(f.apply(element));
        }
        return retList;
    }

    public static <T, K> Map<K, T> toMap(List<T> list, Function<T, K> idFunction) {
        final Map<K, T> map = new HashMap<>(list.size());
        for (T element : list) {
            final K id = idFunction.apply(element);
            if (map.containsKey(id)) {
                throw new IllegalArgumentException("Map already contains an entry for: " + id);
            }
            map.put(id, element);
        }
        return map;
    }

    public static <T, K> Map<K, List<T>> toMultiMap(List<T> list, Function<T, K> idFunction) {
        final Map<K, List<T>> map = new HashMap<>(list.size());
        for (T element : list) {
            final K id = idFunction.apply(element);
            List<T> existingList = map.get(id);
            if (existingList == null) {
                existingList = new ArrayList<>();
                map.put(id, existingList);
            }
            existingList.add(element);
        }
        return map;
    }

    public static <T> List<T> filter(List<T> list, Predicate<? super T> predicate) {
        return list.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T> boolean containsAny(List<T> list, List<T> elements) {
        for (T element : elements) {
            if (list.contains(element)) {
                return true;
            }
        }
        return false;
    }

    public static <T> List<T> takeAllExcept(List<T> list, T excluded) {
        return list.stream().filter(element -> !element.equals(excluded)).collect(Collectors.toList());
    }
}
