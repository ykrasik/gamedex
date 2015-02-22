package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.opt.Opt;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface ConfigManager {
    Opt<Path> getPrevDirectory();
    void setPrevDirectory(Path prevDirectory);

    boolean isShowLog();
    void setShowLog(boolean show);
}
