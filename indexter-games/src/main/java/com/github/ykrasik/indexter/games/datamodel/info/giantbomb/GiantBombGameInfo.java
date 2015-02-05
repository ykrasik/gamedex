package com.github.ykrasik.indexter.games.datamodel.info.giantbomb;

import com.github.ykrasik.indexter.games.datamodel.ImageData;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class GiantBombGameInfo {
    @NonNull private final String name;
    @NonNull private final Optional<String> description;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final String apiDetailsUrl;
    @NonNull private final Optional<ImageData> thumbnail;
    @NonNull private final Optional<ImageData> poster;
    @NonNull private final List<String> genres;
}
