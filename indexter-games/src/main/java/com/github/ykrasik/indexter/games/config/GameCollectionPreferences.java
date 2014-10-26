package com.github.ykrasik.indexter.games.config;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameCollectionPreferences {
    Optional<File> getPrevDirectory();
    void setPrevDirectory(File prevDirectory);

    Map<Path, GamePlatform> getLibraries();
    void setLibraries(Map<Path, GamePlatform> libraries);
}
