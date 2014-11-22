package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public class GameComparators {
    private static final Comparator<LocalGameInfo> NAME_COMPARATOR = (o1, o2) ->
        o1.getGameInfo().getName().compareTo(o2.getGameInfo().getName());

    private static final Comparator<LocalGameInfo> CRITIC_SCORE_COMPARATOR = (o1, o2) ->
        compareWithNameFallback(o1, o2, info -> info.getGameInfo().getCriticScore());

    private static final Comparator<LocalGameInfo> USER_SCORE_COMPARATOR = (o1, o2) ->
        compareWithNameFallback(o1, o2, info -> info.getGameInfo().getUserScore());

    private static final Comparator<LocalGameInfo> RELEASE_DATE_COMPARATOR = (o1, o2) -> {
        final Optional<LocalDate> releaseDate1 = o1.getGameInfo().getReleaseDate();
        final Optional<LocalDate> releaseDate2 = o2.getGameInfo().getReleaseDate();

        if (releaseDate1.isPresent()) {
            if (releaseDate2.isPresent()) {
                return releaseDate1.get().compareTo(releaseDate2.get());
            } else {
                return 1;
            }
        } else {
            return -1;
        }
    };


    public static Comparator<LocalGameInfo> dateAddedComparator() {
        // FIXME: Implement
        return nameComparator();
    }

    public static Comparator<LocalGameInfo> nameComparator() {
        return NAME_COMPARATOR;
    }

    public static Comparator<LocalGameInfo> criticScoreComparator() {
        return CRITIC_SCORE_COMPARATOR;
    }

    public static Comparator<LocalGameInfo> userScoreComparator() {
        return USER_SCORE_COMPARATOR;
    }

    public static Comparator<LocalGameInfo> releaseDateComparator() {
        return RELEASE_DATE_COMPARATOR;
    }

    private static <T extends Comparable<T>> int compareWithNameFallback(LocalGameInfo o1, LocalGameInfo o2, Function<LocalGameInfo, Optional<T>> fieldExtractor) {
        final Result result = compareOptionals(fieldExtractor.apply(o1), fieldExtractor.apply(o2));
        if (result != Result.INDETERMINABLE) {
            return result.result;
        } else {
            return NAME_COMPARATOR.compare(o1, o2);
        }
    }

    private static <T extends Comparable<T>> Result compareOptionals(Optional<T> o1, Optional<T> o2) {
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

    private static Result fromCompareResult(int result) {
        if (result < 0) {
            return Result.LESSER_THEN;
        }
        if (result > 0) {
            return Result.GREATER_THEN;
        }
        return Result.EQUAL;
    }

    private enum Result {
        EQUAL(0),
        GREATER_THEN(1),
        LESSER_THEN(-1),
        INDETERMINABLE(0);

        private final int result;

        Result(int result) {
            this.result = result;
        }
    }
}
