package com.github.ykrasik.gamedex.persistence;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.persistence.*;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.gs.collections.api.list.ImmutableList;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
// TODO: Inconsistent responsibility between the service and the managers - sometimes managers return from cache,
// TODO: sometimes they query the service. Should be Either Or.
public interface PersistenceService {
    Game addGame(UnifiedGameInfo gameInfo, Path path, GamePlatform platform);
    void deleteGame(Id<Game> id);
    ImmutableList<Game> getAllGames();
    Game getGameById(Id<Game> id);
    boolean hasGameForPath(Path path);

    Library addLibrary(Path path, GamePlatform platform, String name);
    void deleteLibrary(Id<Library> id);
    ImmutableList<Library> getAllLibraries();
    Library getLibraryById(Id<Library> id);
    boolean hasLibraryForPath(Path path);

    void addGameToLibraries(Game game, Iterable<Library> libraries);

    ImmutableList<Genre> getAllGenres();

    ImmutableList<ExcludedPath> getAllExcludedPaths();
    boolean isPathExcluded(Path path);
    ExcludedPath addExcludedPath(Path path);
    void deleteExcludedPath(Id<ExcludedPath> id);
}
