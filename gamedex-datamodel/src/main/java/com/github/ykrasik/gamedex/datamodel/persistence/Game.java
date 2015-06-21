package com.github.ykrasik.gamedex.datamodel.persistence;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.list.ImmutableList;
import lombok.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "path"})
public class Game implements PathEntity {
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

    @NonNull private final LocalDateTime lastModified;

    @NonNull private final ImmutableList<Genre> genres;

    @NonNull private final ImmutableList<Library> libraries;
}
