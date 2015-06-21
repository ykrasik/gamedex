package com.github.ykrasik.gamedex.datamodel.provider;

import com.github.ykrasik.yava.option.Opt;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
public class SearchResult {
    @NonNull private final String detailUrl;
    @NonNull private final String name;
    @NonNull private final Opt<LocalDate> releaseDate;
    @NonNull private final Opt<Double> score;
}
