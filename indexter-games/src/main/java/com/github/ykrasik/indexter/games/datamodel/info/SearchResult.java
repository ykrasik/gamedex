package com.github.ykrasik.indexter.games.datamodel.info;

import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
public class SearchResult {
    @NonNull private final String detailUrl;
    @NonNull private final String name;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> score;
}
