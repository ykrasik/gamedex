package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.opt.Opt;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
// TODO: Rename this class.
public interface GameCollectionConfig {
    Opt<Path> getPrevDirectory();
    void setPrevDirectory(Path prevDirectory);
}
