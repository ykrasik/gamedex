package com.github.ykrasik.gamedex.common.util;

import com.github.ykrasik.gamedex.common.exception.FunctionThrows;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.ykrasik.gamedex.common.exception.ExceptionWrappers.rethrow;

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

    public static <T, K> Map<K, List<T>> toMultiMap(Collection<T> collection, Function<T, K> idFunction) {
        final Map<K, List<T>> map = new HashMap<>(collection.size());
        for (T element : collection) {
            final K id = idFunction.apply(element);
            List<T> list = map.get(id);
            if (list == null) {
                list = new ArrayList<>();
                map.put(id, list);
            }
            list.add(element);
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
