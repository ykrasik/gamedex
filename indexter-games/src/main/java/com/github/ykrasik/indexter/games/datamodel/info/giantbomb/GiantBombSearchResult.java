package com.github.ykrasik.indexter.games.datamodel.info.giantbomb;

import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class GiantBombSearchResult implements SearchResult {
    @NonNull private final String name;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final String detailUrl;
//    private final Optional<ImageData> thumbnail;

    @Override
    public Optional<Double> getScore() {
        return Optional.empty();
    }
}
