package com.github.ykrasik.indexter.games.datamodel.info.giantbomb;

import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class GiantBombGameInfo implements GameInfo {
    @NonNull private final String name;
    @NonNull private final Optional<String> description;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final String detailUrl;
    @NonNull private final Optional<ImageData> thumbnail;
    @NonNull private final Optional<ImageData> poster;
    @NonNull private final List<String> genres;

    @Override
    public Optional<Double> getCriticScore() {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getUserScore() {
        return Optional.empty();
    }
}
