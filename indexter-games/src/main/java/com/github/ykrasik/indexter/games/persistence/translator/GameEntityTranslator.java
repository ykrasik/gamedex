package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.games.persistence.entity.GameEntity;
import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface GameEntityTranslator {
    LocalGame translate(GameEntity entity);

    GameEntity translate(LocalGame game);
    GameEntity translate(Game game, Path path);
}
