package com.github.ykrasik.gamedex.core.game;

import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.common.optional.OptionalComparators;

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
        OptionalComparators.compareWithFallback(o2, o1, Game::getCriticScore, USER_SCORE_COMPARATOR);

    private static final Comparator<Game> RELEASE_DATE_COMPARATOR = (o1, o2) ->
        compareWithNameFallback(o2, o1, Game::getReleaseDate);

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

    private static <T extends Comparable<? super T>> int compareWithNameFallback(Game o1, Game o2, Function<Game, Optional<T>> fieldExtractor) {
        return OptionalComparators.compareWithFallback(o2, o1, fieldExtractor, NAME_COMPARATOR);
    }
}
