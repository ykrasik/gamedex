package com.github.ykrasik.gamedex.persistence.translator.game;

import com.github.ykrasik.gamedex.common.util.DateUtils;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.persistence.entity.GameEntity;
import com.github.ykrasik.gamedex.persistence.translator.AbstractEntityTranslator;
import com.github.ykrasik.opt.Opt;

import java.nio.file.Paths;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public class GameEntityTranslatorImpl extends AbstractEntityTranslator implements GameEntityTranslator {
    @Override
    public GameEntity translate(UnifiedGameInfo gameInfo) {
        final GameEntity entity = new GameEntity();
        entity.setName(gameInfo.getName());
        entity.setDescription(gameInfo.getDescription().getOrElseNull());
        entity.setReleaseDate(gameInfo.getReleaseDate().map(DateUtils::fromLocalDate).getOrElseNull());
        entity.setCriticScore(gameInfo.getCriticScore().getOrElseNull());
        entity.setUserScore(gameInfo.getUserScore().getOrElseNull());
        entity.setMetacriticDetailUrl(gameInfo.getMetacriticDetailUrl());
        entity.setGiantBombDetailUrl(gameInfo.getGiantBombDetailUrl().getOrElseNull());
        entity.setThumbnailData(gameInfo.getThumbnail().map(ImageData::getRawData).getOrElseNull());
        entity.setPosterData(gameInfo.getPoster().map(ImageData::getRawData).getOrElseNull());
        return entity;
    }

    @Override
    public GameEntity translate(Game game) {
        final GameEntity entity = new GameEntity();
        entity.setId(game.getId().getId());
        entity.setPath(game.getPath().toString());
        entity.setMetacriticDetailUrl(game.getMetacriticDetailUrl());
        entity.setGiantBombDetailUrl(game.getGiantBombDetailUrl().getOrElseNull());
        entity.setName(game.getName());
        entity.setPlatform(game.getPlatform());
        entity.setDescription(game.getDescription().getOrElseNull());
        entity.setReleaseDate(game.getReleaseDate().map(DateUtils::fromLocalDate).getOrElseNull());
        entity.setCriticScore(game.getCriticScore().getOrElseNull());
        entity.setUserScore(game.getUserScore().getOrElseNull());
        entity.setThumbnailData(game.getThumbnail().map(ImageData::getRawData).getOrElseNull());
        entity.setPosterData(game.getPoster().map(ImageData::getRawData).getOrElseNull());
        entity.setLastModified(DateUtils.fromLocalDateTime(game.getLastModified()));
        return entity;
    }

    @Override
    public Game translate(GameEntity entity, List<Genre> genres, List<Library> libraries) {
        return Game.builder()
            .id(new Id<>(entity.getId()))
            .path(Paths.get(entity.getPath()))
            .metacriticDetailUrl(entity.getMetacriticDetailUrl())
            .giantBombDetailUrl(Opt.ofNullable(entity.getGiantBombDetailUrl()))
            .name(entity.getName())
            .platform(entity.getPlatform())
            .description(Opt.ofNullable(entity.getDescription()))
            .releaseDate(Opt.ofNullable(entity.getReleaseDate()).map(DateUtils::toLocalDate))
            .criticScore(Opt.ofNullable(entity.getCriticScore()))
            .userScore(Opt.ofNullable(entity.getUserScore()))
            .thumbnail(Opt.ofNullable(entity.getThumbnailData()).map(ImageData::of))
            .poster(Opt.ofNullable(entity.getPosterData()).map(ImageData::of))
            .lastModified(DateUtils.toLocalDateTime(entity.getLastModified()))
            .genres(genres)
            .libraries(libraries)
            .build();
    }
}
