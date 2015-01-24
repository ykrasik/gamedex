package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;
import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.DateUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameEntityTranslatorImpl extends AbstractEntityTranslator implements GameEntityTranslator {
    private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Joiner JOINER = Joiner.on(',').skipNulls();

    @Override
    public LocalGame translate(GameEntity entity) {
        final Id<LocalGame> id = translateId(entity.getId());
        final Path path = Paths.get(entity.getPath());
        final LocalDateTime lastModified = DateUtils.toLocalDateTime(entity.getLastModified());

        final String name = entity.getName();
        final GamePlatform platform = entity.getPlatform();
        final Optional<String> description = Optional.ofNullable(entity.getDescription());
        final Optional<LocalDate> releaseDate = Optional.ofNullable(entity.getReleaseDate()).map(DateUtils::toLocalDate);
        final Optional<Double> criticScore = Optional.ofNullable(entity.getCriticScore());
        final Optional<Double> userScore = Optional.ofNullable(entity.getUserScore());
        final List<String> genres = translateGenres(entity.getGenres());
        final Optional<String> giantBombApiDetailUrl = Optional.ofNullable(entity.getGiantBombApiDetailUrl());
        final Optional<byte[]> thumbnailData = Optional.ofNullable(entity.getThumbnailData());
        final Optional<byte[]> posterData = Optional.ofNullable(entity.getPosterData());

        final Game game = new Game(
            name, platform, description, releaseDate, criticScore, userScore, genres, giantBombApiDetailUrl,
            thumbnailData, posterData
        );

        return new LocalGame(id, path, lastModified, game);
    }

    @Override
    public GameEntity translate(LocalGame game) {
        final GameEntity entity = translate(game.getGame(), game.getPath());
        entity.setId(game.getId().getId());
        return entity;
    }

    @Override
    public GameEntity translate(Game game, Path path) {
        final GameEntity entity = new GameEntity();
        entity.setPath(path.toString());
        entity.setName(game.getName());
        entity.setPlatform(game.getPlatform());
        entity.setDescription(game.getDescription().orElse(null));
        entity.setReleaseDate(game.getReleaseDate().map(DateUtils::fromLocalDate).orElse(null));
        entity.setCriticScore(game.getCriticScore().orElse(null));
        entity.setUserScore(game.getUserScore().orElse(null));
        entity.setGenres(translateGenres(game.getGenres()));
        entity.setGiantBombApiDetailUrl(game.getGiantBombApiDetailsUrl().orElse(null));
        entity.setThumbnailData(game.getThumbnailData().orElse(null));
        entity.setPosterData(game.getPosterData().orElse(null));
        return entity;
    }

    private List<String> translateGenres(String genres) {
        return genres != null ? SPLITTER.splitToList(genres) : Collections.emptyList();
    }

    private String translateGenres(List<String> genres) {
        return !genres.isEmpty() ? JOINER.join(genres) : null;
    }
}
