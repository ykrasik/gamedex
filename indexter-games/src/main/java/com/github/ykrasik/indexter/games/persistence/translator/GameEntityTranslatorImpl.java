package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.indexter.util.DateUtils;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameEntityTranslatorImpl extends AbstractEntityTranslator implements GameEntityTranslator {
    @Override
    public GameEntity translate(GameInfo gameInfo) {
        final GameEntity entity = new GameEntity();
        entity.setName(gameInfo.getName());
        entity.setDescription(gameInfo.getDescription().orElse(null));
        entity.setReleaseDate(gameInfo.getReleaseDate().map(DateUtils::fromLocalDate).orElse(null));
        entity.setCriticScore(gameInfo.getCriticScore().orElse(null));
        entity.setUserScore(gameInfo.getUserScore().orElse(null));
        entity.setGiantBombApiDetailUrl(gameInfo.getGiantBombApiDetailsUrl().orElse(null));
        entity.setUrl(gameInfo.getUrl());
        entity.setThumbnailData(gameInfo.getThumbnail().map(ImageData::getRawData).orElse(null));
        entity.setPosterData(gameInfo.getPoster().map(ImageData::getRawData).orElse(null));
        return entity;
    }

    @Override
    public GameEntity translate(Game game) {
        final GameEntity entity = new GameEntity();
        entity.setId(game.getId().getId());
        entity.setPath(game.getPath().toString());
        entity.setName(game.getName());
        entity.setPlatform(game.getPlatform());
        entity.setDescription(game.getDescription().orElse(null));
        entity.setReleaseDate(game.getReleaseDate().map(DateUtils::fromLocalDate).orElse(null));
        entity.setCriticScore(game.getCriticScore().orElse(null));
        entity.setUserScore(game.getUserScore().orElse(null));
        entity.setGiantBombApiDetailUrl(game.getGiantBombApiDetailsUrl().orElse(null));
        entity.setUrl(game.getUrl());
        entity.setThumbnailData(game.getThumbnail().map(ImageData::getRawData).orElse(null));
        entity.setPosterData(game.getPoster().map(ImageData::getRawData).orElse(null));
        entity.setLastModified(DateUtils.fromLocalDateTime(game.getLastModified()));
        return entity;
    }

    @Override
    public Game translate(GameEntity entity, List<Genre> genres) {
        return Game.builder()
            .id(new Id<>(entity.getId()))
            .path(Paths.get(entity.getPath()))
            .name(entity.getName())
            .platform(entity.getPlatform())
            .description(Optional.ofNullable(entity.getDescription()))
            .releaseDate(Optional.ofNullable(entity.getReleaseDate()).map(DateUtils::toLocalDate))
            .criticScore(Optional.ofNullable(entity.getCriticScore()))
            .userScore(Optional.ofNullable(entity.getUserScore()))
            .giantBombApiDetailsUrl(Optional.ofNullable(entity.getGiantBombApiDetailUrl()))
            .url(entity.getUrl())
            .thumbnail(Optional.ofNullable(entity.getThumbnailData()).map(ImageData::of))
            .poster(Optional.ofNullable(entity.getPosterData()).map(ImageData::of))
            .lastModified(DateUtils.toLocalDateTime(entity.getLastModified()))
            .genres(genres)
            .build();
    }
}
