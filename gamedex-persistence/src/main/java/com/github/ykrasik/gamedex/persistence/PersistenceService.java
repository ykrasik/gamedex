package com.github.ykrasik.gamedex.persistence;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.persistence.*;
import com.github.ykrasik.gamedex.datamodel.provider.UnifiedGameInfo;
import com.github.ykrasik.opt.Opt;
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

    Opt<ImageData> getThumbnail(Id<Game> id);
    Opt<ImageData> getPoster(Id<Game> id);

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
