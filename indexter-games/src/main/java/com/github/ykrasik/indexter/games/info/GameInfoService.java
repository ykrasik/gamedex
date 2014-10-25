package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoService {
    // FIXME: Return a future
    List<GameRawBriefInfo> searchGames(String name, GamePlatform gamePlatform) throws Exception;

    // FIXME: Add type-safety, for metacritic this is name, for giantbomb this is a url.
    // FIXME: Return a future
    Optional<GameInfo> getGameInfo(String moreDetailsId, GamePlatform gamePlatform) throws Exception;
}

