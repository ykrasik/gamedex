package com.github.ykrasik.gamedex.core.game;

import com.github.ykrasik.gamedex.common.comparator.FieldComparator;
import com.github.ykrasik.gamedex.common.comparator.OptionalComparator;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
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

    @Getter private static final Comparator<Game> userScoreAsc = rawUserScoreAsc.or(rawCriticScoreAsc).or(rawReleaseDateAsc).or(nameAsc);
    @Getter private static final Comparator<Game> userScoreDesc = userScoreAsc.reversed();

    @Getter private static final Comparator<Game> criticScoreAsc = rawCriticScoreAsc.or(rawUserScoreAsc).or(rawReleaseDateAsc).or(nameAsc);
    @Getter private static final Comparator<Game> criticScoreDesc = criticScoreAsc.reversed();

    @Getter private static final Comparator<Game> releaseDateAsc = rawReleaseDateAsc.or(nameAsc);
    @Getter private static final Comparator<Game> releaseDateDesc = releaseDateAsc.reversed();
}
