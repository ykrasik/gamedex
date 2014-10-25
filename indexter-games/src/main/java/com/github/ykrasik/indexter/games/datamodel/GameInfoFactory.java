package com.github.ykrasik.indexter.games.datamodel;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
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
                                String thumbnailUrl) throws IOException {
        final byte[] thumbnailData = fetchThumbnail(thumbnailUrl);
        return new GameInfo(
            name, description, gamePlatform, releaseDate, criticScore, userScore,
            genres, publishers, developers, url, thumbnailData
        );
    }

    private static byte[] fetchThumbnail(String urlStr) throws IOException {
        final URL url = new URL(urlStr);
        return IOUtils.toByteArray(url);
    }
}
