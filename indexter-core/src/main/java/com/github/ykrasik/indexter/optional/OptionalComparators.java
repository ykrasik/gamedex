package com.github.ykrasik.indexter.optional;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class OptionalComparators {
    private OptionalComparators() { }

    public static <T extends Comparable<? super T>> Result compare(Optional<T> o1, Optional<T> o2) {
        if (o1.isPresent()) {
            if (o2.isPresent()) {
                return fromCompareResult(o1.get().compareTo(o2.get()));
            } else {
                return Result.GREATER_THEN;
            }
        } else {
            if (o2.isPresent()) {
                return Result.LESSER_THEN;
            } else {
                return Result.INDETERMINABLE;
            }
        }
    }

    public static <O, T extends Comparable<? super T>> int compareWithFallback(O o1,
                                                                               O o2,
                                                                               Function<O, Optional<T>> fieldExtractor,
                                                                               Comparator<O> fallback) {
        final Result result = compare(fieldExtractor.apply(o1), fieldExtractor.apply(o2));
        if (result != Result.INDETERMINABLE) {
            return result.getResult();
        } else {
            return fallback.compare(o1, o2);
        }
    }

    private static Result fromCompareResult(int result) {
        if (result < 0) {
            return Result.LESSER_THEN;
        }
        if (result > 0) {
            return Result.GREATER_THEN;
        }
        return Result.EQUAL;
    }

    public enum Result {
        EQUAL(0),
        GREATER_THEN(1),
        LESSER_THEN(-1),
        INDETERMINABLE(0);

        private final int result;

        Result(int result) {
            this.result = result;
        }

        public int getResult() {
            return result;
        }
    }
}
