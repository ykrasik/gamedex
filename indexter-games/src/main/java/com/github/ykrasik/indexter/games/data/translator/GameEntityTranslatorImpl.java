package com.github.ykrasik.indexter.games.data.translator;

import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameEntityTranslatorImpl extends AbstractEntityTranslator implements GameEntityTranslator {
    @Override
    public GameInfo translate(GameInfoEntity entity) {
        final String name = entity.getName();
        final Optional<String> description = Optional.ofNullable(entity.getDescription());
        final GamePlatform gamePlatform = entity.getGamePlatform();
        final Optional<LocalDate> releaseDate = Optional.ofNullable(entity.getReleaseDate()).map(this::translateDate);
        final double criticScore = entity.getCriticScore();
        final double userScore = entity.getUserScore();
        final List<String> genres = Collections.<String>emptyList();
        final List<String> publishers = Collections.<String>emptyList();
        final List<String> developers = Collections.<String>emptyList();
        final Optional<String> url = Optional.ofNullable(entity.getUrl());
        final Optional<byte[]> thumbnailData = Optional.ofNullable(entity.getThumbnailData());

        return new GameInfo(
            name, description, gamePlatform, releaseDate, criticScore, userScore,
            genres, publishers, developers, url, thumbnailData
        );
    }

    @Override
    public GameInfoEntity translate(GameInfo info) {
        final String name = info.getName();
        final String description = info.getDescription().orElse(null);
        final GamePlatform gamePlatform = info.getGamePlatform();
        final Date releaseDate = info.getReleaseDate().map(this::translateDate).orElse(null);
        final double criticScore = info.getCriticScore();
        final double userScore = info.getUserScore();
        final String url = info.getUrl().orElse(null);
        final byte[] thumbnailData = info.getThumbnailData().orElse(null);

        return new GameInfoEntity(name, description, gamePlatform, releaseDate, criticScore, userScore, url, thumbnailData);
    }
}
