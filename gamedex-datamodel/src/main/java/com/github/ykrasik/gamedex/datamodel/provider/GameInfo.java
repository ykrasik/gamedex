package com.github.ykrasik.gamedex.datamodel.provider;

import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.list.ImmutableList;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
public class GameInfo {
    @NonNull private final String detailUrl;
    @NonNull private final String name;
    @NonNull private final Opt<String> description;
    @NonNull private final Opt<LocalDate> releaseDate;
    @NonNull private final Opt<Double> criticScore;
    @NonNull private final Opt<Double> userScore;
    @NonNull private final Opt<ImageData> thumbnail;
    @NonNull private final Opt<ImageData> poster;
    @NonNull private final ImmutableList<String> genres;
}
