package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameComparators {
    public static Comparator<LocalGameInfo> dateAddedComparator() {
        // FIXME: Implement
        return nameComparator();
    }

    public static Comparator<LocalGameInfo> nameComparator() {
        return (o1, o2) -> o1.getGameInfo().getName().compareTo(o2.getGameInfo().getName());
    }

    public static Comparator<LocalGameInfo> criticScoreComparator() {
        return (o1, o2) -> (int) (o1.getGameInfo().getCriticScore() - o2.getGameInfo().getCriticScore());
    }

    public static Comparator<LocalGameInfo> userScoreComparator() {
        return (o1, o2) -> (int) (o1.getGameInfo().getUserScore() - o2.getGameInfo().getUserScore());
    }

    public static Comparator<LocalGameInfo> releaseDateComparator() {
        return (o1, o2) -> {
            final Optional<LocalDate> releaseDate1 = o1.getGameInfo().getReleaseDate();
            final Optional<LocalDate> releaseDate2 = o2.getGameInfo().getReleaseDate();

            if (releaseDate1.isPresent() && !releaseDate2.isPresent()) {
                return 1;
            }
            if (releaseDate2.isPresent() && !releaseDate1.isPresent()) {
                return -1;
            }
            return releaseDate1.get().compareTo(releaseDate2.get());
        };
    }
}
