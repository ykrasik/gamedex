package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoService<T extends SearchResult, E extends GameInfo> {
    List<T> searchGames(String name, GamePlatform platform) throws Exception;

    Optional<E> getGameInfo(T searchResult) throws Exception;
}
