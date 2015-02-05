package com.github.ykrasik.indexter.games.datamodel.info.giantbomb;

import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class GiantBombSearchResult {
    @NonNull private final String name;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final String apiDetailUrl;
//    private final Optional<ImageData> thumbnail;
}
