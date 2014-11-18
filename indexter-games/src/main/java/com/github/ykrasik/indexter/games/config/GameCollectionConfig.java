package com.github.ykrasik.indexter.games.config;

import com.github.ykrasik.indexter.games.datamodel.Library;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Yevgeny Krasik
 */
public interface GameCollectionConfig {
    Optional<File> getPrevDirectory();
    void setPrevDirectory(File prevDirectory);

    // TODO: Consider encapsulating these getters with getValues and containsKey.
    Map<Path, Library> getLibraries();
    void addLibrary(Library library);

    Map<Path, Library> getSubLibraries();
    void addSubLibrary(Library subLibrary);

    Set<Path> getExcludedPaths();
    void addExcludedPath(Path path);
}
