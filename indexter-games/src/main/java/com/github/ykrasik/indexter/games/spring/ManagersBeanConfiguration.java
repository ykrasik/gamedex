package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManagerImpl;
import com.github.ykrasik.indexter.games.manager.flow.dialog.DialogManager;
import com.github.ykrasik.indexter.games.manager.flow.dialog.DialogManagerImpl;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.game.GameManagerImpl;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManagerImpl;
import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import javafx.stage.Stage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ManagersBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public GameCollectionConfig gameCollectionConfig() throws IOException {
        preloader.setMessage("Loading config...");
        return new GameCollectionConfigImpl();
    }

    @Bean
    public DialogManager choiceProvider(Stage stage) {
        return new DialogManagerImpl(stage);
    }

    @Bean
    public FlowManager flowManager(LibraryManager libraryManager,
                                   GameManager gameManager,
                                   MetacriticGameInfoService metacriticInfoService,
                                   GiantBombGameInfoService giantBombInfoService,
                                   DialogManager dialogManager) {
        return new FlowManagerImpl(libraryManager, gameManager, metacriticInfoService, giantBombInfoService, dialogManager);
    }

    @Bean
    public GameManager gameManager(PersistenceService persistenceService) {
        preloader.setMessage("Loading game manager...");
        return new GameManagerImpl(persistenceService);
    }

    @Bean
    public LibraryManager libraryManager(GameCollectionConfig config, PersistenceService persistenceService) {
        preloader.setMessage("Loading library manager...");
        return new LibraryManagerImpl(config, persistenceService);
    }
}
