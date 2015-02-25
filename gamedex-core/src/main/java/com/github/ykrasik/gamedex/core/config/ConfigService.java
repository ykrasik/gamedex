package com.github.ykrasik.gamedex.core.config;

import com.github.ykrasik.gamedex.core.config.type.GameSort;
import com.github.ykrasik.gamedex.core.config.type.GameWallImageDisplay;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface ConfigService {
    ObjectProperty<Opt<Path>> prevDirectoryProperty();

    BooleanProperty autoSkipProperty();
    BooleanProperty showLogProperty();

    ObjectProperty<GameWallImageDisplay> gameWallImageDisplayProperty();
    ObjectProperty<GameSort> gameSortProperty();
}
