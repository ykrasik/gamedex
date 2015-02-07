package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo2;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;

import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface GameEntityTranslator {
    GameEntity translate(GameInfo2 gameInfo);
    GameEntity translate(Game game);

    Game translate(GameEntity entity, List<Genre> genres);
}
