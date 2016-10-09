package com.github.ykrasik.gamedex.core.manager.game;

import com.github.ykrasik.gamedex.datamodel.Game;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.Genre;
import com.github.ykrasik.gamedex.datamodel.Library;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.provider.GameData;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface GameManager {
    Game addGame(GameData gameInfo, Path path, GamePlatform platform);

    void deleteGame(Game game);
    void deleteGames(Collection<Game> games);

    ObservableList<Game> getAllGames();
    Game getGameById(Id<Game> id);
    boolean isGame(Path path);

    ObservableList<Genre> getAllGenres();

    ReadOnlyListProperty<Game> gamesProperty();

    void nameFilter(String name);
    void noNameFilter();

    void genreFilter(List<Genre> genres);
    void noGenreFilter();

    void libraryFilter(Library library);
    void noLibraryFilter();

    void sort(GameSort sort);
}
