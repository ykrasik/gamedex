package com.github.ykrasik.gamedex.core.manager.game;

import com.github.ykrasik.gamedex.common.comparator.FieldComparator;
import com.github.ykrasik.gamedex.common.comparator.OptionalComparator;
import com.github.ykrasik.gamedex.common.comparator.TwoFieldOptionalComparator;
import com.github.ykrasik.gamedex.datamodel.Game;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public final class GameComparators {
    private GameComparators() { }

    private static final OptionalComparator<Game, Double> rawUserScoreAsc = OptionalComparator.of(Game::getUserScore);
    private static final OptionalComparator<Game, Double> rawCriticScoreAsc = OptionalComparator.of(Game::getCriticScore);
    private static final OptionalComparator<Game, LocalDate> rawReleaseDateAsc = OptionalComparator.of(Game::getReleaseDate);

    @Getter private static final Comparator<Game> nameAsc = FieldComparator.of(Game::getName);
    @Getter private static final Comparator<Game> nameDesc = nameAsc.reversed();

    @Getter private static final Comparator<Game> lastModifiedAsc = FieldComparator.of(Game::getLastModified);
    @Getter private static final Comparator<Game> lastModifiedDesc = lastModifiedAsc.reversed();

    @Getter private static final Comparator<Game> userScoreAsc = rawUserScoreAsc.thenComparing(rawCriticScoreAsc).thenComparing(rawReleaseDateAsc).thenComparing(nameAsc);
    @Getter private static final Comparator<Game> userScoreDesc = userScoreAsc.reversed();

    @Getter private static final Comparator<Game> criticScoreAsc = rawCriticScoreAsc.thenComparing(rawUserScoreAsc).thenComparing(rawReleaseDateAsc).thenComparing(nameAsc);
    @Getter private static final Comparator<Game> criticScoreDesc = criticScoreAsc.reversed();

    private static final Comparator<Game> rawMinScoreAsc = new TwoFieldOptionalComparator<Game, Double, Double>(Game::getCriticScore, Game::getUserScore) {
        @Override
        protected int doCompare(Double o1Field1, Double o2Field1, Double o1Field2, Double o2Field2) {
            return Double.compare(Math.min(o1Field1, o1Field2), Math.min(o2Field1, o2Field2));
        }
    };

    @Getter private static final Comparator<Game> minScoreAsc = rawMinScoreAsc.thenComparing(rawReleaseDateAsc).thenComparing(nameAsc);
    @Getter private static final Comparator<Game> minScoreDesc = minScoreAsc.reversed();

    private static final Comparator<Game> rawAvgScoreAsc = new TwoFieldOptionalComparator<Game, Double, Double>(Game::getCriticScore, Game::getUserScore) {
        @Override
        protected int doCompare(Double o1Field1, Double o2Field1, Double o1Field2, Double o2Field2) {
            final double avg1 = (o1Field1 + o1Field2) / 2;
            final double avg2 = (o2Field1 + o2Field2) / 2;
            return Double.compare(avg1, avg2);
        }
    };

    @Getter private static final Comparator<Game> avgScoreAsc = rawAvgScoreAsc.thenComparing(rawReleaseDateAsc).thenComparing(nameAsc);
    @Getter private static final Comparator<Game> avgScoreDesc = avgScoreAsc.reversed();

    @Getter private static final Comparator<Game> releaseDateAsc = rawReleaseDateAsc.thenComparing(nameAsc);
    @Getter private static final Comparator<Game> releaseDateDesc = releaseDateAsc.reversed();
}
