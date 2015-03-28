package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.service.screen.detail.GameDetailsScreen;
import com.github.ykrasik.gamedex.core.service.screen.search.GameSearchScreen;
import com.github.ykrasik.gamedex.core.service.screen.settings.SettingsScreen;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ScreenBeanConfiguration {
    @Bean
    public GameSearchScreen gameSearchScreen(StageManager stageManager) {
        return JavaFxUtils.callLaterIfNecessary(() -> new GameSearchScreen(stageManager));
    }

    @Bean
    public SettingsScreen settingsScreen(ConfigService configService, StageManager stageManager) {
        return JavaFxUtils.callLaterIfNecessary(() -> new SettingsScreen(configService, stageManager));
    }

    @Bean
    public GameDetailsScreen gameDetailsScreen(ImageService imageService, ActionService actionService, StageManager stageManager) {
        return JavaFxUtils.callLaterIfNecessary(() -> new GameDetailsScreen(imageService, actionService, stageManager));
    }
}
