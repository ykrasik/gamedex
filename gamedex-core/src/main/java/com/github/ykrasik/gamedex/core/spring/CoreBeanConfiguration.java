package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.common.spring.AbstractBeanConfiguration;
import com.github.ykrasik.gamedex.core.config.GameCollectionConfig;
import com.github.ykrasik.gamedex.core.config.GameCollectionConfigImpl;
import com.github.ykrasik.gamedex.core.controller.ControllerProvider;
import com.github.ykrasik.gamedex.core.dialog.DialogManager;
import com.github.ykrasik.gamedex.core.dialog.DialogManagerImpl;
import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.exclude.ExcludedPathManagerImpl;
import com.github.ykrasik.gamedex.core.exclude.debug.ExcludedPathDebugCommands;
import com.github.ykrasik.gamedex.core.flow.FlowManager;
import com.github.ykrasik.gamedex.core.flow.FlowManagerImpl;
import com.github.ykrasik.gamedex.core.flow.debug.FlowManagerDebugCommands;
import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.game.GameManagerImpl;
import com.github.ykrasik.gamedex.core.game.debug.GameManagerDebugCommands;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import com.github.ykrasik.gamedex.core.library.LibraryManagerImpl;
import com.github.ykrasik.gamedex.core.library.debug.LibraryManagerDebugCommands;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import com.github.ykrasik.gamedex.provider.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.gamedex.provider.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class CoreBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public Parent mainScene(ControllerProvider controllerProvider) throws IOException {
        preloader.info("Loading FXML...");
        final FXMLLoader loader = new FXMLLoader(UIResources.getMainFxml());
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
    public DialogManager choiceProvider(Stage stage) {
        return new DialogManagerImpl(stage);
    }

    @Bean
    public FlowManager flowManager(LibraryManager libraryManager,
                                   ExcludedPathManager excludedPathManager,
                                   GameManager gameManager,
                                   MetacriticGameInfoService metacriticInfoService,
                                   GiantBombGameInfoService giantBombInfoService,
                                   DialogManager dialogManager) {
        return new FlowManagerImpl(
            libraryManager,
            excludedPathManager,
            gameManager,
            metacriticInfoService,
            giantBombInfoService,
            dialogManager
        );
    }

    @Bean
    public FlowManagerDebugCommands flowManagerDebugCommands(FlowManager flowManager, LibraryManager libraryManager) {
        return new FlowManagerDebugCommands(flowManager, libraryManager);
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
}
