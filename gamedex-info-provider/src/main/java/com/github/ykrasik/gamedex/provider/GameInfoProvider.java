package com.github.ykrasik.gamedex.provider;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.gs.collections.api.list.ImmutableList;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoProvider {
    GameInfoProviderInfo getInfo();

    ImmutableList<SearchResult> search(String name, GamePlatform platform) throws Exception;

    GameInfo fetch(SearchResult searchResult) throws Exception;
}
