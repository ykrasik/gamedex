package com.github.ykrasik.gamedex.common.comparator;

import com.github.ykrasik.yava.option.Opt;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TwoFieldOptionalComparator<T, F1 extends Comparable<? super F1>, F2 extends Comparable<? super F2>> implements Comparator<T> {
    @NonNull private final Function<T, Opt<F1>> fieldExtractor1;
    @NonNull private final Function<T, Opt<F2>> fieldExtractor2;

    @Override
    public int compare(T o1, T o2) {
        final Opt<F1> o1Field1 = fieldExtractor1.apply(o1);
        final Opt<F1> o2Field1 = fieldExtractor1.apply(o2);
        boolean field1Undefined = false;
        if (o1Field1.isDefined()) {
            if (!o2Field1.isDefined()) {
                return 1;
            }
        } else if (o2Field1.isDefined()) {
            return -1;
        } else {
            field1Undefined = true;
        }

        // At this point, field1 is either defined or undefined in both objects.
        final Opt<F2> o1Field2 = fieldExtractor2.apply(o1);
        final Opt<F2> o2Field2 = fieldExtractor2.apply(o2);
        if (o1Field2.isDefined()) {
            if (!o2Field2.isDefined()) {
                return 1;
            }
        } else if (o2Field2.isDefined()) {
            return -1;
        } else {
            if (!field1Undefined) {
                return o1Field1.get().compareTo(o2Field1.get());
            } else {
                return 0;
            }
        }

        if (!field1Undefined) {
            return doCompare(o1Field1.get(), o2Field1.get(), o1Field2.get(), o2Field2.get());
        } else {
            return o1Field2.get().compareTo(o2Field2.get());
        }

    }

    protected abstract int doCompare(F1 o1Field1, F1 o2Field1, F2 o1Field2, F2 o2Field2);
}
