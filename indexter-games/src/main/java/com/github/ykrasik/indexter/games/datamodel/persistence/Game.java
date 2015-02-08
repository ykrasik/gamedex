package com.github.ykrasik.indexter.games.datamodel.persistence;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.id.Id;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Builder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @NonNull private final String name;
    @NonNull private final GamePlatform platform;
    @NonNull private final Optional<String> description;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> criticScore;
    @NonNull private final Optional<Double> userScore;
    @NonNull private final Optional<String> giantBombApiDetailsUrl;
    @NonNull private final String url;

    @NonNull private final Optional<ImageData> thumbnail;
    @NonNull private final Optional<ImageData> poster;

    @NonNull private final LocalDateTime lastModified;

    @NonNull private final List<Genre> genres;

    public static Game from(Id<Game> id,
                            Path path,
                            GamePlatform platform,
                            LocalDateTime lastModified,
                            GameInfo info,
                            List<Genre> genres) {
        return builder()
            .id(id)
            .path(path)
            .platform(platform)
            .name(info.getName())
            .lastModified(lastModified)
            .description(info.getDescription())
            .releaseDate(info.getReleaseDate())
            .criticScore(info.getCriticScore())
            .userScore(info.getUserScore())
            .giantBombApiDetailsUrl(Optional.of(info.getDetailUrl()))
            .url(info.getUrl())
            .thumbnail(info.getThumbnail())
            .poster(info.getPoster())
            .genres(genres)
            .build();
    }
}
