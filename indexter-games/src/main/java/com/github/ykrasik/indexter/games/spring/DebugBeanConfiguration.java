package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.debug.*;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoServiceImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoServiceImpl;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DebugBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public MetacriticDebugCommands metacriticDebugCommands(MetacriticGameInfoServiceImpl service,
                                                           MetacriticGameInfoClient client,
                                                           ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating Metacritic debug commands...");
        return new MetacriticDebugCommands(service, client, objectMapper);
    }

    @Bean
    public GiantBombDebugCommands giantBombDebugCommands(GiantBombGameInfoServiceImpl service,
                                                         GiantBombGameInfoClient client,
                                                         ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating GiantBomb debug commands...");
        return new GiantBombDebugCommands(service, client, objectMapper);
    }

    @Bean
    public GameManagerDebugCommands gameManagerDebugCommands(GameManager gameManager) {
        preloader.setMessage("Instantiating GameManager debug commands...");
        return new GameManagerDebugCommands(gameManager);
    }

    @Bean
    public LibraryManagerDebugCommands libraryManagerDebugCommands(LibraryManager libraryManager) {
        preloader.setMessage("Instantiating LibraryManager debug commands...");
        return new LibraryManagerDebugCommands(libraryManager);
    }

    @Bean
    public FlowManagerDebugCommands flowManagerDebugCommands(FlowManager flowManager, LibraryManager libraryManager) {
        preloader.setMessage("Instantiating FlowManager debug commands...");
        return new FlowManagerDebugCommands(flowManager, libraryManager);
    }

    @Bean
    public ConfigDebugCommands configDebugCommands(GameCollectionConfig config) {
        preloader.setMessage("Instantiating config debug commands...");
        return new ConfigDebugCommands(config);
    }
}
