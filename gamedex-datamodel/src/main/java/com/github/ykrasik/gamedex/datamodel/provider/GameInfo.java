package com.github.ykrasik.gamedex.datamodel.provider;

import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.opt.Opt;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

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
    @NonNull private final List<String> genres;
}
