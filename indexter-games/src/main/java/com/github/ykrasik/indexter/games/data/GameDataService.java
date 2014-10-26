package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataService {
    void add(LocalGameInfo info) throws DataException;

    Optional<LocalGameInfo> get(Path path) throws DataException;

    Collection<LocalGameInfo> getAll() throws DataException;

    void addListener(GameDataListener listener);
}
