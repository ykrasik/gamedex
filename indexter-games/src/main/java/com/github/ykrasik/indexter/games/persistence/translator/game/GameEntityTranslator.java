package com.github.ykrasik.indexter.games.persistence.translator.game;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;

import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface GameEntityTranslator {
    GameEntity translate(GameInfo gameInfo);
    GameEntity translate(Game game);

    Game translate(GameEntity entity, List<Genre> genres);
}
