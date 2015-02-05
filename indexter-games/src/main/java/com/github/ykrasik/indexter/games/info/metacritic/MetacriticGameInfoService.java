package com.github.ykrasik.indexter.games.info.metacritic;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticSearchResult;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface MetacriticGameInfoService {
    List<MetacriticSearchResult> searchGames(String name, GamePlatform platform) throws Exception;

    Optional<MetacriticGameInfo> getGameInfo(String name, GamePlatform platform) throws Exception;
}
