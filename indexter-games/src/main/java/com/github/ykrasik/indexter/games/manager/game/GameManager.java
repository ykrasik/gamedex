package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.GameSort;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.id.Id;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameManager {
    LocalGame addGame(Game game, Path path) throws DataException;
    void deleteGame(LocalGame game) throws DataException;

    LocalGame getGameById(Id<LocalGame> id);
    Optional<LocalGame> getGameByPath(Path path);
    boolean isGameMapped(Path path);

    ObservableList<LocalGame> getAllGames();
    void sort(GameSort sort);

    ReadOnlyObjectProperty<ObservableList<LocalGame>> itemsProperty();
}
