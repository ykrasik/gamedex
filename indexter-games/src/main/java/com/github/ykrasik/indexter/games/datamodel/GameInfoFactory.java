package com.github.ykrasik.indexter.games.datamodel;

import com.github.ykrasik.indexter.util.UrlUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public final class GameInfoFactory {
    private GameInfoFactory() {
    }

    public static GameInfo from(String name,
                                Optional<String> description,
                                GamePlatform gamePlatform,
                                Optional<LocalDate> releaseDate,
                                double criticScore,
                                double userScore,
                                List<String> genres,
                                List<String> publishers,
                                List<String> developers,
                                String url,
                                Optional<String> thumbnailUrl) throws IOException {
        final Optional<byte[]> thumbnailData;
        if (thumbnailUrl.isPresent()) {
            thumbnailData = Optional.of(UrlUtils.fetchData(thumbnailUrl.get()));
        } else {
            thumbnailData = Optional.empty();
        }

        return new GameInfo(
            name, description, gamePlatform, releaseDate, criticScore, userScore,
            genres, publishers, developers, url, thumbnailData
        );
    }
}
