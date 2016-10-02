package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.common.spring.AbstractBeanConfiguration;
import com.github.ykrasik.gamedex.core.manager.config.ConfigManager;
import com.github.ykrasik.gamedex.core.manager.config.ConfigManagerImpl;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManagerImpl;
import com.github.ykrasik.gamedex.core.manager.exclude.debug.ExcludedPathDebugCommands;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManagerImpl;
import com.github.ykrasik.gamedex.core.manager.game.debug.GameManagerDebugCommands;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManagerImpl;
import com.github.ykrasik.gamedex.core.manager.library.debug.LibraryManagerDebugCommands;
import com.github.ykrasik.gamedex.core.manager.path.PathManager;
import com.github.ykrasik.gamedex.core.manager.path.PathManagerImpl;
import com.github.ykrasik.gamedex.core.manager.provider.GameInfoProviderManager;
import com.github.ykrasik.gamedex.core.manager.provider.GameInfoProviderManagerImpl;
import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.manager.stage.StageManagerImpl;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.service.screen.search.GameSearchScreen;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import com.gitlab.ykrasik.gamedex.provider.DataProvider;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class CoreBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public StageManager stageManager(Stage stage) {
        return new StageManagerImpl(stage);
    }

    @Bean
    public GameManager gameManager(PersistenceService persistenceService) {
        preloader.message("Loading game manager...");
        return new GameManagerImpl(persistenceService);
    }

    @Bean
    public GameManagerDebugCommands gameManagerDebugCommands(GameManager gameManager) {
        return new GameManagerDebugCommands(gameManager);
    }

    @Bean
    public LibraryManager libraryManager(PersistenceService persistenceService) {
        preloader.message("Loading library manager...");
        return new LibraryManagerImpl(persistenceService);
    }

    @Bean
    public LibraryManagerDebugCommands libraryManagerDebugCommands(LibraryManager libraryManager) {
        return new LibraryManagerDebugCommands(libraryManager);
    }

    @Bean
    public ExcludedPathManager excludedPathManager(PersistenceService persistenceService) {
        preloader.message("Loading excluded path manager...");
        return new ExcludedPathManagerImpl(persistenceService);
    }

    @Bean
    public ExcludedPathDebugCommands excludedPathDebugCommands(ExcludedPathManager excludedPathManager) {
        return new ExcludedPathDebugCommands(excludedPathManager);
    }

    @Qualifier("metacriticManager")
    @Bean
    public GameInfoProviderManager metacriticManager(ConfigService configService,
                                                     GameSearchScreen gameSearchScreen,
                                                     @Qualifier("metacriticGameInfoProvider") DataProvider metacriticGameInfoProvider) {
        return new GameInfoProviderManagerImpl(configService, gameSearchScreen, metacriticGameInfoProvider);
    }

    @Qualifier("giantBombManager")
    @Bean
    public GameInfoProviderManager giantBombManager(ConfigService configService,
                                                    GameSearchScreen gameSearchScreen,
                                                    @Qualifier("giantBombGameInfoProvider") DataProvider giantBombGameInfoProvider) {
        return new GameInfoProviderManagerImpl(configService, gameSearchScreen, giantBombGameInfoProvider);
    }

    @Bean
    public PathManager pathManager(ConfigService configService,
                                   DialogService dialogService,
                                   GameManager gameManager,
                                   LibraryManager libraryManager,
                                   ExcludedPathManager excludedPathManager,
                                   @Qualifier("metacriticManager") GameInfoProviderManager metacriticManager,
                                   @Qualifier("giantBombManager") GameInfoProviderManager giantBombManager) {
        return new PathManagerImpl(
            configService,
            dialogService,
            gameManager,
            libraryManager,
            excludedPathManager,
            metacriticManager,
            giantBombManager
        );
    }

    @Bean
    public ConfigManager configManager() {
        preloader.message("Loading config...");
        return new ConfigManagerImpl();
    }
}
