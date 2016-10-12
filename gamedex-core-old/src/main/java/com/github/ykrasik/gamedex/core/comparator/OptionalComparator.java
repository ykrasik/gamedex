package com.github.ykrasik.gamedex.core.comparator;

import com.github.ykrasik.yava.option.Opt;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OptionalComparator<T, E extends Comparable<? super E>> implements Comparator<T> {
    @NonNull private final Function<T, Opt<E>> fieldExtractor;

    public OptionalComparator<T, E> or(Comparator<T> fallback) {
        return new OptionalComparator<T, E>(fieldExtractor) {
            @Override
            public int compare(T o1, T o2) {
                final int result = OptionalComparator.this.compare(o1, o2);
                return result != 0 ? result : fallback.compare(o1, o2);
            }
        };
    }

    @Override
    public OptionalComparator<T, E> reversed() {
        return new OptionalComparator<T, E>(fieldExtractor) {
            @Override
            public int compare(T o1, T o2) {
                return OptionalComparator.this.compare(o2, o1);
            }
        };
    }

    @Override
    public int compare(T o1, T o2) {
        final Opt<E> opt1 = fieldExtractor.apply(o1);
        final Opt<E> opt2 = fieldExtractor.apply(o2);
        return compareOpt(opt1, opt2);
    }

    private int compareOpt(Opt<E> o1, Opt<E> o2) {
        if (o1.isDefined()) {
            if (o2.isDefined()) {
                return o1.get().compareTo(o2.get());
            } else {
                return 1;
            }
        } else {
            if (o2.isDefined()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public static <T, E extends Comparable<? super E>> OptionalComparator<T, E> of(@NonNull Function<T, Opt<E>> fieldExtractor) {
        return new OptionalComparator<>(fieldExtractor);
    }
}
