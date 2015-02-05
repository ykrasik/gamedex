package com.github.ykrasik.indexter.games.datamodel.info.metacritic;

import com.github.ykrasik.indexter.games.datamodel.ImageData;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class MetacriticGameInfo {
    @NonNull private final String name;
    @NonNull private final Optional<String> description;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> criticScore;
    @NonNull private final Optional<Double> userScore;
    @NonNull private final String url;
    @NonNull private final Optional<ImageData> thumbnail;
    @NonNull private final Optional<String> genre;
}
