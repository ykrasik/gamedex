package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.games.info.GameDetailedInfo;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataService {
    void add(GameDetailedInfo info) throws IllegalArgumentException;

    Collection<GameDetailedInfo> getAll();
}
