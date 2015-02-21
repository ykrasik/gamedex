package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.common.spring.AbstractBeanConfiguration;
import com.github.ykrasik.gamedex.core.config.GameCollectionConfig;
import com.github.ykrasik.gamedex.core.config.GameCollectionConfigImpl;
import com.github.ykrasik.gamedex.core.controller.ControllerProvider;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManagerImpl;
import com.github.ykrasik.gamedex.core.manager.exclude.debug.ExcludedPathDebugCommands;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManagerImpl;
import com.github.ykrasik.gamedex.core.manager.game.debug.GameManagerDebugCommands;
import com.github.ykrasik.gamedex.core.manager.info.GameInfoProviderManager;
import com.github.ykrasik.gamedex.core.manager.info.GameInfoProviderManagerImpl;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManagerImpl;
import com.github.ykrasik.gamedex.core.manager.library.debug.LibraryManagerDebugCommands;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.action.ActionServiceImpl;
import com.github.ykrasik.gamedex.core.service.action.debug.ActionManagerDebugCommands;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.service.dialog.DialogServiceImpl;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class CoreBeanConfiguration extends AbstractBeanConfiguration {
    // FIXME: This should be done outside of spring, to allow adding context menus through fxml.
    @Bean
    public Parent mainScene(ControllerProvider controllerProvider) throws IOException {
        preloader.info("Loading FXML...");
        final FXMLLoader loader = new FXMLLoader(UIResources.mainFxml());
        loader.setControllerFactory(controllerProvider::getController);
        return loader.load();
    }

    @Bean
    public Parent debugConsole(List<DebugCommands> debugCommands) throws IOException {
        preloader.info("Loading debug console...");
        final ShellFileSystem fileSystem = new ShellFileSystem();
        debugCommands.forEach(fileSystem::processAnnotationsOfObject);
        return new ConsoleBuilder(fileSystem).build();
    }

    @Bean
    public GameCollectionConfig gameCollectionConfig() throws IOException {
        preloader.info("Loading config...");
        return new GameCollectionConfigImpl();
    }

    @Bean
    public DialogService choiceProvider(Stage stage) {
        return new DialogServiceImpl(stage);
    }

    @Bean
    public ActionService actionManager(GameCollectionConfig config,
                                       DialogService dialogService,
                                       GameManager gameManager,
                                       LibraryManager libraryManager,
                                       ExcludedPathManager excludedPathManager,
                                       GameInfoProviderManager metacriticManager,
                                       GameInfoProviderManager giantBombManager) {
        return new ActionServiceImpl(
            config,
            dialogService,
            gameManager,
            libraryManager,
            excludedPathManager,
            metacriticManager,
            giantBombManager
        );
    }

    @Bean
    public ActionManagerDebugCommands flowManagerDebugCommands(ActionService actionService, LibraryManager libraryManager) {
        return new ActionManagerDebugCommands(actionService, libraryManager);
    }

    @Bean
    public GameManager gameManager(PersistenceService persistenceService) {
        preloader.info("Loading game manager...");
        return new GameManagerImpl(persistenceService);
    }

    @Bean
    public GameManagerDebugCommands gameManagerDebugCommands(GameManager gameManager) {
        return new GameManagerDebugCommands(gameManager);
    }

    @Bean
    public LibraryManager libraryManager(PersistenceService persistenceService) {
        preloader.info("Loading library manager...");
        return new LibraryManagerImpl(persistenceService);
    }

    @Bean
    public LibraryManagerDebugCommands libraryManagerDebugCommands(LibraryManager libraryManager) {
        return new LibraryManagerDebugCommands(libraryManager);
    }

    @Bean
    public ExcludedPathManager excludedPathManager(PersistenceService persistenceService) {
        preloader.info("Loading excluded path manager...");
        return new ExcludedPathManagerImpl(persistenceService);
    }

    @Bean
    public ExcludedPathDebugCommands excludedPathDebugCommands(ExcludedPathManager excludedPathManager) {
        return new ExcludedPathDebugCommands(excludedPathManager);
    }

    @Qualifier("metacriticManager")
    @Bean
    public GameInfoProviderManager metacriticManager(DialogService dialogService,
                                                     @Qualifier("metacriticGameInfoProvider") GameInfoProvider metacriticGameInfoProvider) {
        return new GameInfoProviderManagerImpl(dialogService, metacriticGameInfoProvider, false);
    }

    @Qualifier("giantBombManager")
    @Bean
    public GameInfoProviderManager giantBombManager(DialogService dialogService,
                                                    @Qualifier("giantBombGameInfoProvider") GameInfoProvider giantBombGameInfoProvider) {
        return new GameInfoProviderManagerImpl(dialogService, giantBombGameInfoProvider, true);
    }
}
