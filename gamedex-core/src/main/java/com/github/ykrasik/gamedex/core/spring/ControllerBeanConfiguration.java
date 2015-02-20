package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.core.action.ActionService;
import com.github.ykrasik.gamedex.core.controller.*;
import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ControllerBeanConfiguration {
    @Bean
    public ControllerProvider controllerProvider(List<Controller> controllers) {
        final Map<Class<?>, Controller> controllerMap = ListUtils.toMap(controllers, Object::getClass);
        return new ControllerProvider(controllerMap);
    }

    @Bean
    public MainController mainController(ActionService actionService,
                                         GameManager gameManager,
                                         LibraryManager libraryManager) {
        return new MainController(actionService, gameManager, libraryManager);
    }

    @Bean
    public GameController gameController(ActionService actionService,
                                         GameManager gameManager,
                                         LibraryManager libraryManager) {
        return new GameController(actionService, gameManager, libraryManager);
    }

    @Bean
    public GameWallController gameWallController() {
        return new GameWallController();
    }

    @Bean
    public GameListController gameListController() {
        return new GameListController();
    }

    @Bean
    public GameSideBarController gameSideBarController() {
        return new GameSideBarController();
    }

    @Bean
    public LibraryController libraryController(LibraryManager libraryManager) {
        return new LibraryController(libraryManager);
    }

    @Bean
    public ExcludedController excludedController(ExcludedPathManager excludedPathManager) {
        return new ExcludedController(excludedPathManager);
    }
}
