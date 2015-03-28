package com.github.ykrasik.gamedex.core.service.config;

import com.github.ykrasik.gamedex.core.manager.game.GameSort;
import com.github.ykrasik.gamedex.core.ui.gridview.GameWallImageDisplay;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.ObjectProperty;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface ConfigService {
    ObjectProperty<Boolean> autoSkipProperty();
    boolean isAutoSkip();

    ObjectProperty<Boolean> showLogProperty();
    boolean isShowLog();

    ObjectProperty<Number> logDividerPosition();
    double getLogDividerPosition();

    ObjectProperty<GameWallImageDisplay> gameWallImageDisplayProperty();
    GameWallImageDisplay getGameWallImageDisplay();

    ObjectProperty<GameSort> gameSortProperty();
    GameSort getGameSort();

    ObjectProperty<Opt<Path>> prevDirectoryProperty();
    Opt<Path> getPrevDirectory();
}
