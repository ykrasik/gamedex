package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.game.GameManagerImpl;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManagerImpl;
import com.github.ykrasik.indexter.games.manager.scan.ScanManager;
import com.github.ykrasik.indexter.games.manager.scan.ScanManagerImpl;
import com.github.ykrasik.indexter.games.manager.scan.choice.ChoiceProvider;
import com.github.ykrasik.indexter.games.manager.scan.choice.DialogChoiceProvider;
import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public ScanManager scanManager(LibraryManager libraryManager,
                                   GameManager gameManager,
                                   @Qualifier("metacriticInfoService") GameInfoService metacriticInfoService,
                                   @Qualifier("giantBombInfoService") GameInfoService giantBombInfoService,
                                   ChoiceProvider choiceProvider) {
        return new ScanManagerImpl(libraryManager, gameManager, metacriticInfoService, giantBombInfoService, choiceProvider);
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
