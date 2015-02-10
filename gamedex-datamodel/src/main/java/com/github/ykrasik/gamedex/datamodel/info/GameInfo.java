package com.github.ykrasik.gamedex.datamodel.info;

import com.github.ykrasik.gamedex.datamodel.ImageData;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
public class GameInfo {
    @NonNull private final String detailUrl;
    @NonNull private final String name;
    @NonNull private final Optional<String> description;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> criticScore;
    @NonNull private final Optional<Double> userScore;
    @NonNull private final String url;
    @NonNull private final Optional<ImageData> thumbnail;
    @NonNull private final Optional<ImageData> poster;
    @NonNull private final List<String> genres;
}
