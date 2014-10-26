package com.github.ykrasik.indexter.util;

import java.util.*;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class ListUtils {
    private ListUtils() {
    }

    public static <T, A> List<A> map(Collection<T> list, Function<T, A> f) {
        final List<A> retList = new ArrayList<>(list.size());
        for (T element : list) {
            retList.add(f.apply(element));
        }
        return retList;
    }
}
