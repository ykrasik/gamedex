package com.github.ykrasik.gamedex.provider;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;

import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoProvider {
    GameInfoProviderType getProviderType();

    List<SearchResult> searchGames(String name, GamePlatform platform) throws Exception;

    Opt<GameInfo> getGameInfo(SearchResult searchResult) throws Exception;
}
