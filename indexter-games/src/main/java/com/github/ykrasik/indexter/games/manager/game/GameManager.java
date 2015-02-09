package com.github.ykrasik.indexter.games.manager.game;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.id.Id;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface GameManager {
    Game addGame(GameInfo gameInfo, Path path, GamePlatform platform);

    void deleteGame(Game game);
    void deleteGames(Collection<Game> games);

    ObservableList<Game> getAllGames();
    Game getGameById(Id<Game> id);
    boolean isGame(Path path);

    ObservableList<Genre> getAllGenres();

    ReadOnlyListProperty<Game> gamesProperty();

    void sort(GameSort sort);

    void nameFilter(String name);
    void noNameFilter();

    void genreFilter(List<Genre> genres);
    void noGenreFilter();
}
