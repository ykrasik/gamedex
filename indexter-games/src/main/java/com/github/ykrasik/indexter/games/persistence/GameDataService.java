package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.games.datamodel.GameDetailedInfo;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataService {
    void add(GameDetailedInfo info) throws IllegalArgumentException;

    void addAll(Collection<GameDetailedInfo> infos) throws IllegalArgumentException;

    Collection<GameDetailedInfo> getAll();

    void addListener(GameDataListener listener);
}
