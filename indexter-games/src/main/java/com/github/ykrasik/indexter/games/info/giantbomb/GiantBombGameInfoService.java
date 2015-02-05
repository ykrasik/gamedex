package com.github.ykrasik.indexter.games.info.giantbomb;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombGameInfoService {
    List<GiantBombSearchResult> searchGames(String name, GamePlatform platform) throws Exception;

    Optional<GiantBombGameInfo> getGameInfo(String apiDetailUrl) throws Exception;
}
