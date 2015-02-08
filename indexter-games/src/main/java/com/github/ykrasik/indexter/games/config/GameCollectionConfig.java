package com.github.ykrasik.indexter.games.config;

import java.io.File;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameCollectionConfig {
    Optional<File> getPrevDirectory();
    void setPrevDirectory(File prevDirectory);
}
