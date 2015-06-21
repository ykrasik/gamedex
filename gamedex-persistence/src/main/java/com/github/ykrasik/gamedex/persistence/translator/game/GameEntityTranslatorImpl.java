package com.github.ykrasik.gamedex.persistence.translator.game;

import com.github.ykrasik.gamedex.common.util.DateUtils;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.persistence.entity.GameEntity;
import com.github.ykrasik.gamedex.persistence.translator.AbstractEntityTranslator;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.list.ImmutableList;

import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
public class GameEntityTranslatorImpl extends AbstractEntityTranslator implements GameEntityTranslator {
    @Override
    public GameEntity translate(UnifiedGameInfo gameInfo) {
        return new GameEntity()
            .name(gameInfo.getName())
            .description(gameInfo.getDescription().getOrElseNull())
            .releaseDate(gameInfo.getReleaseDate().map(DateUtils::fromLocalDate).getOrElseNull())
            .criticScore(gameInfo.getCriticScore().getOrElseNull())
            .userScore(gameInfo.getUserScore().getOrElseNull())
            .metacriticDetailUrl(gameInfo.getMetacriticDetailUrl())
            .giantBombDetailUrl(gameInfo.getGiantBombDetailUrl().getOrElseNull());
    }

    @Override
    public GameEntity translate(Game game) {
        return new GameEntity()
            .id(game.getId().getId())
            .path(game.getPath().toString())
            .metacriticDetailUrl(game.getMetacriticDetailUrl())
            .giantBombDetailUrl(game.getGiantBombDetailUrl().getOrElseNull())
            .name(game.getName())
            .platform(game.getPlatform())
            .description(game.getDescription().getOrElseNull())
            .releaseDate(game.getReleaseDate().map(DateUtils::fromLocalDate).getOrElseNull())
            .criticScore(game.getCriticScore().getOrElseNull())
            .userScore(game.getUserScore().getOrElseNull())
            .lastModified(DateUtils.fromLocalDateTime(game.getLastModified()));
    }

    @Override
    public Game translate(GameEntity entity, ImmutableList<Genre> genres, ImmutableList<Library> libraries) {
        return Game.builder()
            .id(new Id<>(entity.id()))
            .path(Paths.get(entity.path()))
            .metacriticDetailUrl(entity.metacriticDetailUrl())
            .giantBombDetailUrl(Opt.ofNullable(entity.giantBombDetailUrl()))
            .name(entity.name())
            .platform(entity.platform())
            .description(Opt.ofNullable(entity.description()))
            .releaseDate(Opt.ofNullable(entity.releaseDate()).map(DateUtils::toLocalDate))
            .criticScore(Opt.ofNullable(entity.criticScore()))
            .userScore(Opt.ofNullable(entity.userScore()))
            .lastModified(DateUtils.toLocalDateTime(entity.lastModified()))
            .genres(genres)
            .libraries(libraries)
            .build();
    }
}
