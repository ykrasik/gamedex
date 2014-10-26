package com.github.ykrasik.indexter.games.data.translator;

import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    public LocalGameInfo translate(GameInfoEntity entity) {
        final Path path = Paths.get(entity.getPath());

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

        final GameInfo gameInfo = new GameInfo(
            name, description, gamePlatform, releaseDate, criticScore, userScore,
            genres, publishers, developers, url, thumbnailData
        );

        return new LocalGameInfo(path, gameInfo);
    }

    @Override
    public GameInfoEntity translate(LocalGameInfo localInfo) {
        final String path = localInfo.getPath().toString();

        final GameInfo gameInfo = localInfo.getGameInfo();
        final String name = gameInfo.getName();
        final String description = gameInfo.getDescription().orElse(null);
        final GamePlatform gamePlatform = gameInfo.getGamePlatform();
        final Date releaseDate = gameInfo.getReleaseDate().map(this::translateDate).orElse(null);
        final double criticScore = gameInfo.getCriticScore();
        final double userScore = gameInfo.getUserScore();
        final String url = gameInfo.getUrl().orElse(null);
        final byte[] thumbnailData = gameInfo.getThumbnailData().orElse(null);

        return new GameInfoEntity(
            path, name, description, gamePlatform, releaseDate, criticScore, userScore, url, thumbnailData
        );
    }
}
