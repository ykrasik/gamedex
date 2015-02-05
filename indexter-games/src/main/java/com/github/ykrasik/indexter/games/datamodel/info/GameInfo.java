package com.github.ykrasik.indexter.games.datamodel.info;

import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticGameInfo;
import com.github.ykrasik.indexter.util.Optionals;
import lombok.*;

import java.time.LocalDate;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(of = {"name", "releaseDate"})
public class GameInfo {
    @NonNull private final String name;
    @NonNull private final Optional<String> description;
    @NonNull private final Optional<LocalDate> releaseDate;
    @NonNull private final Optional<Double> criticScore;
    @NonNull private final Optional<Double> userScore;
    @NonNull private final Optional<String> giantBombApiDetailsUrl;
    @NonNull private final String url;
    @NonNull private final Optional<ImageData> thumbnail;
    @NonNull private final Optional<ImageData> poster;
    @NonNull private final List<String> genres;

    public static GameInfo merge(MetacriticGameInfo metacriticGameInfo, GiantBombGameInfo giantBombGameInfo) {
        // TODO: Consider only using giantBomb genre if present.
        final Set<String> genres = new HashSet<>();
        metacriticGameInfo.getGenre().ifPresent(genres::add);
        genres.addAll(giantBombGameInfo.getGenres());

        final Optional<ImageData> thumbnail = Optionals.or(giantBombGameInfo.getThumbnail(), metacriticGameInfo.getThumbnail());

        return new GameInfo(
            metacriticGameInfo.getName(),
            Optionals.or(giantBombGameInfo.getDescription(), metacriticGameInfo.getDescription()),
            Optionals.or(metacriticGameInfo.getReleaseDate(), giantBombGameInfo.getReleaseDate()),
            metacriticGameInfo.getCriticScore(),
            metacriticGameInfo.getUserScore(),
            Optional.of(giantBombGameInfo.getApiDetailsUrl()),
            metacriticGameInfo.getUrl(),
            thumbnail,
            Optionals.or(giantBombGameInfo.getPoster(), thumbnail),
            new ArrayList<>(genres)
        );
    }

    public static GameInfo from(MetacriticGameInfo metacriticGameInfo) {
        final List<String> genres = new ArrayList<>(1);
        metacriticGameInfo.getGenre().ifPresent(genres::add);

        return new GameInfo(
            metacriticGameInfo.getName(),
            metacriticGameInfo.getDescription(),
            metacriticGameInfo.getReleaseDate(),
            metacriticGameInfo.getCriticScore(),
            metacriticGameInfo.getUserScore(),
            Optional.empty(),
            metacriticGameInfo.getUrl(),
            metacriticGameInfo.getThumbnail(),
            metacriticGameInfo.getThumbnail(),
            genres
        );
    }
}
