package com.github.ykrasik.indexter.games.data.translator;

import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

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
    private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Joiner JOINER = Joiner.on(',').skipNulls();

    @Override
    public LocalGameInfo translate(GameInfoEntity entity) {
        final int id = entity.getId();
        final Path path = Paths.get(entity.getPath());

        final String name = entity.getName();
        final GamePlatform platform = entity.getPlatform();
        final Optional<String> description = Optional.ofNullable(entity.getDescription());
        final Optional<LocalDate> releaseDate = Optional.ofNullable(entity.getReleaseDate()).map(this::translateDate);
        final Optional<Double> criticScore = Optional.ofNullable(entity.getCriticScore());
        final Optional<Double> userScore = Optional.ofNullable(entity.getUserScore());
        final List<String> genres = translateGenres(entity.getGenres());
        final Optional<String> giantBombApiDetailUrl = Optional.ofNullable(entity.getGiantBombApiDetailUrl());
        final Optional<byte[]> thumbnailData = Optional.ofNullable(entity.getThumbnailData());
        final Optional<byte[]> posterData = Optional.ofNullable(entity.getPosterData());

        final GameInfo gameInfo = new GameInfo(
            name, platform, description, releaseDate, criticScore, userScore, genres, giantBombApiDetailUrl,
            thumbnailData, posterData
        );

        return new LocalGameInfo(id, path, gameInfo);
    }

    private List<String> translateGenres(String genres) {
        return genres != null ? SPLITTER.splitToList(genres) : Collections.emptyList();
    }

    @Override
    public GameInfoEntity translate(LocalGameInfo localInfo) {
        final String path = localInfo.getPath().toString();

        final GameInfo gameInfo = localInfo.getGameInfo();
        final String name = gameInfo.getName();
        final GamePlatform platform = gameInfo.getPlatform();
        final String description = gameInfo.getDescription().orElse(null);
        final Date releaseDate = gameInfo.getReleaseDate().map(this::translateDate).orElse(null);
        final Double criticScore = gameInfo.getCriticScore().orElse(null);
        final Double userScore = gameInfo.getUserScore().orElse(null);
        final String genres = translateGenres(gameInfo.getGenres());
        final String giantBombApiDetailUrl = gameInfo.getGiantBombApiDetailsUrl().orElse(null);
        final byte[] thumbnailData = gameInfo.getThumbnailData().orElse(null);
        final byte[] posterData = gameInfo.getPosterData().orElse(null);

        return new GameInfoEntity(
            path, name, description, platform, releaseDate, criticScore, userScore, genres, giantBombApiDetailUrl,
            thumbnailData, posterData
        );
    }

    private String translateGenres(List<String> genres) {
        return !genres.isEmpty() ? JOINER.join(genres) : null;
    }
}
