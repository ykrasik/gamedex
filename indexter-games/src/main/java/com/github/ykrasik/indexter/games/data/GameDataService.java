package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataService {
    void add(GameInfo info) throws DataException;

    void addAll(Collection<GameInfo> infos) throws DataException;

    Collection<GameInfo> getAll() throws DataException;

    void addListener(GameDataListener listener);
}
