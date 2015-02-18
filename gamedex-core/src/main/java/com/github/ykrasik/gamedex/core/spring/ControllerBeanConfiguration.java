package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.core.config.GameCollectionConfig;
import com.github.ykrasik.gamedex.core.controller.*;
import com.github.ykrasik.gamedex.core.dialog.DialogManager;
import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.flow.FlowManager;
import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import javafx.stage.Stage;
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
    public MainController mainController(Stage stage,
                                         GameCollectionConfig config,
                                         DialogManager dialogManager,
                                         FlowManager flowManager,
                                         GameManager gameManager,
                                         LibraryManager libraryManager) {
        return new MainController(stage, config, dialogManager, flowManager, gameManager, libraryManager);
    }

    @Bean
    public GameController gameController(FlowManager flowManager,
                                         GameManager gameManager,
                                         LibraryManager libraryManager) {
        return new GameController(flowManager, gameManager, libraryManager);
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
