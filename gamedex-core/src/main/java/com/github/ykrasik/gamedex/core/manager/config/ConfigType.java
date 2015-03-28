package com.github.ykrasik.gamedex.core.manager.config;

import com.github.ykrasik.gamedex.core.manager.game.GameSort;
import com.github.ykrasik.gamedex.core.ui.gridview.GameWallImageDisplay;
import com.github.ykrasik.opt.Opt;

/**
 * @author Yevgeny Krasik
 */
public enum ConfigType {
    PREV_DIRECTORY(Opt.absent()),

    AUTO_SKIP(false),

    SHOW_LOG(true),
    LOG_DIVIDER_POSITION(0.98),

    GAME_WALL_IMAGE_DISPLAY(GameWallImageDisplay.FIT),
    GAME_SORT(GameSort.NAME_ASC);

    private final Object defaultValue;

    ConfigType(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
