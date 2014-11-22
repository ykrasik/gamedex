package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataService {
    void add(LocalGameInfo info) throws DataException;

    void delete(int id) throws DataException;

    Optional<LocalGameInfo> get(Path path) throws DataException;

    ObservableList<LocalGameInfo> getAll() throws DataException;

    ReadOnlyObjectProperty<ObservableList<LocalGameInfo>> itemsProperty();
}
