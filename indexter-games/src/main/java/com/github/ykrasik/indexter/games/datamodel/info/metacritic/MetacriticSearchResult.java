package com.github.ykrasik.indexter.games.datamodel.info.metacritic;

import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class MetacriticSearchResult {
    @NonNull private final String name;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> score;
}
