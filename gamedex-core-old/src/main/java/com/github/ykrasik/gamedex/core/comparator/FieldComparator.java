package com.github.ykrasik.gamedex.core.comparator;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldComparator<T, E extends Comparable<? super E>> implements Comparator<T> {
    @NonNull private final Function<T, E> fieldExtractor;

    public FieldComparator<T, E> or(Comparator<T> fallback) {
        return new FieldComparator<T, E>(fieldExtractor) {
            @Override
            public int compare(T o1, T o2) {
                final int result = FieldComparator.this.compare(o1, o2);
                return result != 0 ? result : fallback.compare(o1, o2);
            }
        };
    }

    @Override
    public FieldComparator<T, E> reversed() {
        return new FieldComparator<T, E>(fieldExtractor) {
            @Override
            public int compare(T o1, T o2) {
                return FieldComparator.this.compare(o2, o1);
            }
        };
    }

    @Override
    public int compare(T o1, T o2) {
        final E field1 = fieldExtractor.apply(o1);
        final E field2 = fieldExtractor.apply(o2);
        return field1.compareTo(field2);
    }

    public static <T, E extends Comparable<? super E>> FieldComparator<T, E> of(@NonNull Function<T, E> fieldExtractor) {
        return new FieldComparator<>(fieldExtractor);
    }
}
