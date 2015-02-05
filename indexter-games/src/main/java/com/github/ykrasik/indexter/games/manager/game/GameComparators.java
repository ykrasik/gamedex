package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.games.datamodel.persistence.Game;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class GameComparators {
    private GameComparators() { }

    private static final Comparator<Game> NAME_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());

    private static final Comparator<Game> LAST_MODIFIED_COMPARATOR = (o1, o2) ->
        o1.getLastModified().compareTo(o2.getLastModified());

    private static final Comparator<Game> USER_SCORE_COMPARATOR = (o1, o2) ->
        compareWithNameFallback(o1, o2, Game::getUserScore);

    private static final Comparator<Game> CRITIC_SCORE_COMPARATOR = (o1, o2) ->
        compareWithFallback(o2, o1, Game::getCriticScore, USER_SCORE_COMPARATOR);

    private static final Comparator<Game> RELEASE_DATE_COMPARATOR = (o1, o2) -> {
        // LocalDate does not implement comparable...
        final Optional<LocalDate> releaseDate1 = o1.getReleaseDate();
        final Optional<LocalDate> releaseDate2 = o2.getReleaseDate();

        if (releaseDate2.isPresent()) {
            if (releaseDate1.isPresent()) {
                return releaseDate2.get().compareTo(releaseDate1.get());
            } else {
                return 1;
            }
        } else {
            return -1;
        }
    };

    public static Comparator<Game> nameComparator() {
        return NAME_COMPARATOR;
    }

    public static Comparator<Game> dateAddedComparator() {
        return LAST_MODIFIED_COMPARATOR;
    }

    public static Comparator<Game> criticScoreComparator() {
        return CRITIC_SCORE_COMPARATOR;
    }

    public static Comparator<Game> userScoreComparator() {
        return USER_SCORE_COMPARATOR;
    }

    public static Comparator<Game> releaseDateComparator() {
        return RELEASE_DATE_COMPARATOR;
    }

    private static <T extends Comparable<T>> int compareWithNameFallback(Game o1, Game o2, Function<Game, Optional<T>> fieldExtractor) {
        return compareWithFallback(o2, o1, fieldExtractor, NAME_COMPARATOR);
    }

    private static <T extends Comparable<T>> int compareWithFallback(Game o1,
                                                                     Game o2,
                                                                     Function<Game, Optional<T>> fieldExtractor,
                                                                     Comparator<Game> fallback) {
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
