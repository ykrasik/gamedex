package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.GameInfo2;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.datamodel.persistence.Genre;
import com.github.ykrasik.indexter.games.datamodel.persistence.Library;
import com.github.ykrasik.indexter.id.Id;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface PersistenceService {
    Game addGame(GameInfo2 gameInfo, Path path, GamePlatform platform);
    void deleteGame(Id<Game> id);

    List<Game> getAllGames();
    Game getGameById(Id<Game> id);
    boolean hasGameForPath(Path path);

    Library addLibrary(String name, Path path, GamePlatform platform);
    void deleteLibrary(Id<Library> id);

    List<Library> getAllLibraries();
    Library getLibraryById(Id<Library> id);
    boolean hasLibraryForPath(Path path);

    void addGameToLibrary(Game game, Library library);

    List<Genre> getAllGenres();
}
