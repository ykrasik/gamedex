package com.github.ykrasik.indexter.games.data.translator;

import com.github.ykrasik.indexter.games.data.entity.GameInfoEntity;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;

/**
 * @author Yevgeny Krasik
 */
public interface GameEntityTranslator {
    GameInfo translate(GameInfoEntity entity);

    GameInfoEntity translate(GameInfo info);
}
