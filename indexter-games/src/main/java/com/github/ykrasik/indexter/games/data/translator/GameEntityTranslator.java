package com.github.ykrasik.indexter.games.data.translator;

import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;

/**
 * @author Yevgeny Krasik
 */
public interface GameEntityTranslator {
    LocalGameInfo translate(GameInfoEntity entity);

    GameInfoEntity translate(LocalGameInfo info);
}
