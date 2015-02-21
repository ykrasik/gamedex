package com.github.ykrasik.gamedex.persistence.translator.game;

import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.gamedex.persistence.entity.GameEntity;
import com.gs.collections.api.list.ImmutableList;

/**
 * @author Yevgeny Krasik
 */
public interface GameEntityTranslator {
    GameEntity translate(UnifiedGameInfo gameInfo);
    GameEntity translate(Game game);

    Game translate(GameEntity entity, ImmutableList<Genre> genres, ImmutableList<Library> libraries);
}
