package com.github.ykrasik.gamedex.datamodel.provider;

import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import lombok.*;

import java.time.LocalDate;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class UnifiedGameInfo {
    @NonNull private final String metacriticDetailUrl;
    @NonNull private final Opt<String> giantBombDetailUrl;

    @NonNull private final String name;
    @NonNull private final Opt<String> description;
    @NonNull private final Opt<LocalDate> releaseDate;

    @NonNull private final Opt<Double> criticScore;
    @NonNull private final Opt<Double> userScore;

    @NonNull private final Opt<ImageData> thumbnail;
    @NonNull private final Opt<ImageData> poster;

    @NonNull private final ImmutableList<String> genres;

    public static UnifiedGameInfo from(GameInfo metacriticGameInfo, Opt<GameInfo> giantBombGameInfo) {
        final Opt<ImageData> thumbnail = giantBombGameInfo.flatMap(GameInfo::getThumbnail).orElse(metacriticGameInfo.getThumbnail());
        return UnifiedGameInfo.builder()
            .metacriticDetailUrl(metacriticGameInfo.getDetailUrl())
            .giantBombDetailUrl(giantBombGameInfo.map(GameInfo::getDetailUrl))
            .name(metacriticGameInfo.getName())
            .description(giantBombGameInfo.flatMap(GameInfo::getDescription).orElse(metacriticGameInfo.getDescription()))
            .releaseDate(metacriticGameInfo.getReleaseDate().orElse(giantBombGameInfo.flatMap(GameInfo::getReleaseDate)))
            .criticScore(metacriticGameInfo.getCriticScore())
            .userScore(metacriticGameInfo.getUserScore())
            .thumbnail(thumbnail)
            .poster(giantBombGameInfo.flatMap(GameInfo::getPoster).orElse(thumbnail))
            .genres(giantBombGameInfo.map(GameInfo::getGenres).getOrElse(metacriticGameInfo.getGenres()))
            .build();
    }
}
