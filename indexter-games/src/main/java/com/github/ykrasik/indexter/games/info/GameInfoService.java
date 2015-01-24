package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoService {
    // FIXME: Return a future
    List<GameRawBriefInfo> searchGames(String name, GamePlatform platform) throws Exception;

    // FIXME: Add type-safety, for metacritic this is name, for giantbomb this is a url.
    // FIXME: Return a future
    Optional<Game> getGameInfo(String moreDetailsId, GamePlatform platform) throws Exception;
}

