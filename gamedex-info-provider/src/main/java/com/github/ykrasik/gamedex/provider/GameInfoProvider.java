package com.github.ykrasik.gamedex.provider;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.info.GameInfo;
import com.github.ykrasik.gamedex.datamodel.info.SearchResult;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoProvider {
    GameInfoProviderType getProviderType();

    List<SearchResult> searchGames(String name, GamePlatform platform) throws Exception;

    Optional<GameInfo> getGameInfo(SearchResult searchResult) throws Exception;
}
