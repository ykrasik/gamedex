package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.debug.DataServiceDebugCommands;
import com.github.ykrasik.indexter.games.debug.GiantBombDebugCommands;
import com.github.ykrasik.indexter.games.debug.MetacriticDebugCommands;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DebugBeanConfiguration {
    @Bean
    public MetacriticDebugCommands metacriticDebugCommands(MetacriticGameInfoService service,
                                                           MetacriticGameInfoClient client) {
        return new MetacriticDebugCommands(service, client);
    }

    @Bean
    public GiantBombDebugCommands giantBombDebugCommands(GiantBombGameInfoService service) {
        return new GiantBombDebugCommands(service);
    }

    @Bean
    public DataServiceDebugCommands dataServiceDebugCommands(GameDataService dataService,
                                                             MetacriticGameInfoService service) {
        return new DataServiceDebugCommands(dataService, service);
    }
}
