package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManagerImpl;
import com.github.ykrasik.indexter.games.manager.flow.choice.ChoiceProvider;
import com.github.ykrasik.indexter.games.manager.flow.choice.DialogChoiceProvider;
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
        preloader.setMessage("Instantiating config...");
        return new GameCollectionConfigImpl();
    }

    @Bean
    public ChoiceProvider choiceProvider(Stage stage) {
        return new DialogChoiceProvider(stage);
    }

    @Bean
    public FlowManager flowManager(LibraryManager libraryManager,
                                   GameManager gameManager,
                                   MetacriticGameInfoService metacriticInfoService,
                                   GiantBombGameInfoService giantBombInfoService,
                                   ChoiceProvider choiceProvider) {
        return new FlowManagerImpl(libraryManager, gameManager, metacriticInfoService, giantBombInfoService, choiceProvider);
    }

    @Bean
    public GameManager gameManager(PersistenceService persistenceService) {
        preloader.setMessage("Instantiating game manager...");
        return new GameManagerImpl(persistenceService);
    }

    @Bean
    public LibraryManager libraryManager(PersistenceService persistenceService, GameCollectionConfig config) {
        preloader.setMessage("Instantiating library manager...");
        return new LibraryManagerImpl(persistenceService, config);
    }
}
