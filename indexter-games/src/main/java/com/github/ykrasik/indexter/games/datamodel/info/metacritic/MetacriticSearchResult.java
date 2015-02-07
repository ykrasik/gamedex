package com.github.ykrasik.indexter.games.datamodel.info.metacritic;

import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class MetacriticSearchResult implements SearchResult {
    @NonNull private final String name;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> score;
}
