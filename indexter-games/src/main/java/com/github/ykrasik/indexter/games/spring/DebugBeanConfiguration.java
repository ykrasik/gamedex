package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.debug.*;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DebugBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public MetacriticDebugCommands metacriticDebugCommands(MetacriticGameInfoService service,
                                                           MetacriticGameInfoClient client,
                                                           ObjectMapper objectMapper) {
        return new MetacriticDebugCommands(service, client, objectMapper);
    }

    @Bean
    public GiantBombDebugCommands giantBombDebugCommands(GiantBombGameInfoService service,
                                                         GiantBombGameInfoClient client,
                                                         ObjectMapper objectMapper) {
        return new GiantBombDebugCommands(service, client, objectMapper);
    }

    @Bean
    public GameManagerDebugCommands gameManagerDebugCommands(GameManager gameManager) {
        return new GameManagerDebugCommands(gameManager);
    }

    @Bean
    public LibraryManagerDebugCommands libraryManagerDebugCommands(LibraryManager libraryManager) {
        return new LibraryManagerDebugCommands(libraryManager);
    }

    @Bean
    public FlowManagerDebugCommands flowManagerDebugCommands(FlowManager flowManager, LibraryManager libraryManager) {
        return new FlowManagerDebugCommands(flowManager, libraryManager);
    }

    @Bean
    public ExcludedPathDebugCommands excludedPathDebugCommands(ExcludedPathManager excludedPathManager) {
        return new ExcludedPathDebugCommands(excludedPathManager);
    }
}
