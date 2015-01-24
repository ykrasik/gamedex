package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.exception.DataException;
import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
import com.github.ykrasik.indexter.id.Id;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface PersistenceService {
    LocalGame addGame(Game game, Path path) throws DataException;
    void deleteGame(Id<LocalGame> id) throws DataException;

    LocalGame getGameById(Id<LocalGame> id) throws DataException;
    Optional<LocalGame> getGameByPath(Path path) throws DataException;
    List<LocalGame> getAllGames() throws DataException;

    LocalLibrary addLibrary(Library library) throws DataException;
    void deleteLibrary(Id<LocalLibrary> id) throws DataException;

    LocalLibrary getLibraryById(Id<LocalLibrary> id) throws DataException;
    Optional<LocalLibrary> getLibraryByPath(Path path) throws DataException;
    List<LocalLibrary> getAllLibraries() throws DataException;

    void addGameToLibrary(LocalGame game, LocalLibrary library);
}
