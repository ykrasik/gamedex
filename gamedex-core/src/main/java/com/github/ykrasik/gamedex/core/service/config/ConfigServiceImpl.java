package com.github.ykrasik.gamedex.core.service.config;

import com.github.ykrasik.gamedex.core.manager.config.ConfigManager;
import com.github.ykrasik.gamedex.core.manager.config.ConfigType;
import com.github.ykrasik.gamedex.core.manager.game.GameSort;
import com.github.ykrasik.gamedex.core.ui.gridview.GameWallImageDisplay;
import com.github.ykrasik.yava.option.Opt;
import javafx.beans.property.ObjectProperty;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {
    @NonNull private final ConfigManager configManager;

    @Override
    public ObjectProperty<Boolean> autoSkipProperty() {
        return configManager.property(ConfigType.AUTO_SKIP);
    }

    @Override
    public boolean isAutoSkip() {
        return autoSkipProperty().get();
    }

    @Override
    public ObjectProperty<Boolean> showLogProperty() {
        return configManager.property(ConfigType.SHOW_LOG);
    }

    @Override
    public boolean isShowLog() {
        return showLogProperty().get();
    }

    @Override
    public ObjectProperty<Number> logDividerPosition() {
        return configManager.property(ConfigType.LOG_DIVIDER_POSITION);
    }

    @Override
    public double getLogDividerPosition() {
        return logDividerPosition().get().doubleValue();
    }

    @Override
    public ObjectProperty<GameWallImageDisplay> gameWallImageDisplayProperty() {
        return configManager.property(ConfigType.GAME_WALL_IMAGE_DISPLAY);
    }

    @Override
    public GameWallImageDisplay getGameWallImageDisplay() {
        return gameWallImageDisplayProperty().get();
    }

    @Override
    public ObjectProperty<GameSort> gameSortProperty() {
        return configManager.property(ConfigType.GAME_SORT);
    }

    @Override
    public GameSort getGameSort() {
        return gameSortProperty().get();
    }

    @Override
    public ObjectProperty<Opt<Path>> prevDirectoryProperty() {
        return configManager.property(ConfigType.PREV_DIRECTORY);
    }

    @Override
    public Opt<Path> getPrevDirectory() {
        return prevDirectoryProperty().get();
    }
}
