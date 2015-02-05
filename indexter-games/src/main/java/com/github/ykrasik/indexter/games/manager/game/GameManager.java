package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.id.Id;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * @author Yevgeny Krasik
 */
// TODO: This class doesn't do much, either move more logic here or get rid of it
// TODO: Should probably be in charge of fetching games too, and not flowManager.
public interface GameManager {
    Game addGame(GameInfo gameInfo, Path path, GamePlatform platform) throws DataException;
    void deleteGame(Game game) throws DataException;

    Game getGameById(Id<Game> id);
    boolean isPathMapped(Path path);

    ObservableList<Game> getAllGames();

    void sort(GameSort sort);

    void filter(Predicate<Game> filter);
    void unFilter();

    ReadOnlyObjectProperty<ObservableList<Game>> itemsProperty();
}
