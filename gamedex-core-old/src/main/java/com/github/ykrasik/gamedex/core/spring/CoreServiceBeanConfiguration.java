package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.core.manager.config.ConfigManager;
import com.github.ykrasik.gamedex.core.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.manager.path.PathManager;
import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.action.ActionServiceImpl;
import com.github.ykrasik.gamedex.core.service.action.debug.ActionServiceDebugCommands;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.config.ConfigServiceImpl;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.service.dialog.DialogServiceImpl;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.service.image.ImageServiceImpl;
import com.github.ykrasik.gamedex.core.service.task.TaskService;
import com.github.ykrasik.gamedex.core.service.task.TaskServiceImpl;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import com.github.ykrasik.gamedex.provider.GameInfoProviderInfo;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class CoreServiceBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public TaskService taskService(DialogService dialogService) {
        return new TaskServiceImpl(dialogService);
    }

    @Bean
    public ConfigService configService(ConfigManager configManager) {
        return new ConfigServiceImpl(configManager);
    }

    @Bean
    public DialogService dialogService(Stage stage, StageManager stageManager, ConfigService configService) {
        return new DialogServiceImpl(stage, stageManager, configService);
    }

    @Bean
    public ActionService actionService(ConfigService configService,
                                       TaskService taskService,
                                       DialogService dialogService,
                                       GameManager gameManager,
                                       LibraryManager libraryManager,
                                       ExcludedPathManager excludedPathManager,
                                       PathManager pathManager) {
        return new ActionServiceImpl(
            configService,
            taskService,
            dialogService,
            gameManager,
            libraryManager,
            excludedPathManager,
            pathManager
        );
    }

    @Bean
    public ActionServiceDebugCommands actionServiceDebugCommands(ActionService actionService, LibraryManager libraryManager) {
        return new ActionServiceDebugCommands(actionService, libraryManager);
    }

    @Bean
    public ImageService imageService(PersistenceService persistenceService) {
        return new ImageServiceImpl(persistenceService);
    }

    @Bean
    @Qualifier("metacriticInfo")
    public GameInfoProviderInfo metacriticInfo() {
        return new GameInfoProviderInfo("Metacritic", true, UIResources.metacriticLogo());
    }

    @Bean
    @Qualifier("giantBombInfo")
    public GameInfoProviderInfo giantBombInfo() {
        return new GameInfoProviderInfo("GiantBombInfo", false, UIResources.giantBombLogo());
    }
}
