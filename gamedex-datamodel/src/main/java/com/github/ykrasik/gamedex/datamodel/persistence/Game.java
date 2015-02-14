package com.github.ykrasik.gamedex.datamodel.persistence;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.opt.Opt;
import lombok.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "path"})
public class Game {
    @NonNull private final Id<Game> id;
    @NonNull private final Path path;

    @NonNull private final String metacriticDetailUrl;
    @NonNull private final Opt<String> giantBombDetailUrl;

    @NonNull private final String name;
    @NonNull private final GamePlatform platform;
    @NonNull private final Opt<String> description;
    @NonNull private final Opt<LocalDate> releaseDate;
    @NonNull private final Opt<Double> criticScore;
    @NonNull private final Opt<Double> userScore;

    @NonNull private final Opt<ImageData> thumbnail;
    @NonNull private final Opt<ImageData> poster;

    @NonNull private final LocalDateTime lastModified;

    @NonNull private final List<Genre> genres;

    @NonNull private final List<Library> libraries;
}
