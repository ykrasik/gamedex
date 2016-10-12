package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.controller.ControllerProvider;
import com.github.ykrasik.gamedex.core.controller.MainController;
import com.github.ykrasik.gamedex.core.controller.exclude.ExcludeController;
import com.github.ykrasik.gamedex.core.controller.game.GameController;
import com.github.ykrasik.gamedex.core.controller.game.GameListController;
import com.github.ykrasik.gamedex.core.controller.game.GameWallController;
import com.github.ykrasik.gamedex.core.controller.library.LibraryController;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.service.screen.detail.GameDetailsScreen;
import com.github.ykrasik.gamedex.core.service.screen.settings.SettingsScreen;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.impl.factory.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ControllerBeanConfiguration {
    @Bean
    public ControllerProvider controllerProvider(List<Controller> controllers) {
        final ImmutableMap<Class<?>, Controller> controllerMap = Lists.immutable.ofAll(controllers).groupByUniqueKey(Object::getClass);
        return new ControllerProvider(controllerMap);
    }

    @Bean
    public MainController mainController(ConfigService configService,
                                         ActionService actionService,
                                         GameManager gameManager,
                                         LibraryManager libraryManager,
                                         SettingsScreen settingsScreen) {
        return new MainController(configService, actionService, gameManager, libraryManager, settingsScreen);
    }

    @Bean
    public GameController gameController(ConfigService configService,
                                         ActionService actionService,
                                         GameManager gameManager,
                                         LibraryManager libraryManager) {
        return new GameController(configService, actionService, gameManager, libraryManager);
    }

    @Bean
    public GameWallController gameWallController(ConfigService configService,
                                                 ImageService imageService,
                                                 ActionService actionService,
                                                 GameManager gameManager,
                                                 GameDetailsScreen gameDetailsScreen) {
        return new GameWallController(configService, imageService, actionService, gameManager, gameDetailsScreen);
    }

    @Bean
    public GameListController gameListController(ImageService imageService,
                                                 ActionService actionService,
                                                 GameManager gameManager,
                                                 GameDetailsScreen gameDetailsScreen) {
        return new GameListController(imageService, actionService, gameManager, gameDetailsScreen);
    }

    @Bean
    public LibraryController libraryController(ActionService actionService, LibraryManager libraryManager) {
        return new LibraryController(actionService, libraryManager);
    }

    @Bean
    public ExcludeController excludedController(ActionService actionService, ExcludedPathManager excludedPathManager) {
        return new ExcludeController(actionService, excludedPathManager);
    }
}
