package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.games.datamodel.LocalGame;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public class GameComparators {
    private static final Comparator<LocalGame> NAME_COMPARATOR = (o1, o2) ->
        o1.getGame().getName().compareTo(o2.getGame().getName());

    private static final Comparator<LocalGame> LAST_MODIFIED_COMPARATOR = (o1, o2) ->
        o1.getLastModified().compareTo(o2.getLastModified());

    private static final Comparator<LocalGame> USER_SCORE_COMPARATOR = (o1, o2) ->
        compareWithNameFallback(o1, o2, info -> info.getGame().getUserScore());

    private static final Comparator<LocalGame> CRITIC_SCORE_COMPARATOR = (o1, o2) ->
        compareWithFallback(o1, o2, info -> info.getGame().getCriticScore(), USER_SCORE_COMPARATOR);

    private static final Comparator<LocalGame> RELEASE_DATE_COMPARATOR = (o1, o2) -> {
        final Optional<LocalDate> releaseDate1 = o1.getGame().getReleaseDate();
        final Optional<LocalDate> releaseDate2 = o2.getGame().getReleaseDate();

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


    public static Comparator<LocalGame> nameComparator() {
        return NAME_COMPARATOR;
    }

    public static Comparator<LocalGame> dateAddedComparator() {
        return LAST_MODIFIED_COMPARATOR;
    }

    public static Comparator<LocalGame> criticScoreComparator() {
        return CRITIC_SCORE_COMPARATOR;
    }

    public static Comparator<LocalGame> userScoreComparator() {
        return USER_SCORE_COMPARATOR;
    }

    public static Comparator<LocalGame> releaseDateComparator() {
        return RELEASE_DATE_COMPARATOR;
    }

    private static <T extends Comparable<T>> int compareWithNameFallback(LocalGame o1, LocalGame o2, Function<LocalGame, Optional<T>> fieldExtractor) {
        return compareWithFallback(o1, o2, fieldExtractor, NAME_COMPARATOR);
    }

    private static <T extends Comparable<T>> int compareWithFallback(LocalGame o1,
                                                                     LocalGame o2,
                                                                     Function<LocalGame, Optional<T>> fieldExtractor,
                                                                     Comparator<LocalGame> fallback) {
        final Result result = compareOptionals(fieldExtractor.apply(o1), fieldExtractor.apply(o2));
        if (result != Result.INDETERMINABLE) {
            return result.result;
        } else {
            return fallback.compare(o1, o2);
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
