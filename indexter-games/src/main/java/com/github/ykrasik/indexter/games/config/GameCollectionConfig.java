package com.github.ykrasik.indexter.games.config;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * @author Yevgeny Krasik
 */
public interface GameCollectionConfig {
    Optional<File> getPrevDirectory();
    void setPrevDirectory(File prevDirectory);

    Set<Path> getExcludedPaths();
    void addExcludedPath(Path path);
}
