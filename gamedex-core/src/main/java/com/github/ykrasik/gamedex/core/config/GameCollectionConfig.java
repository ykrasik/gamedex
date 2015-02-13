package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.opt.Opt;

import java.io.File;

/**
 * @author Yevgeny Krasik
 */
public interface GameCollectionConfig {
    Opt<File> getPrevDirectory();
    void setPrevDirectory(File prevDirectory);
}
