package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionPreferencesImpl;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.debug.DataServiceDebugCommands;
import com.github.ykrasik.indexter.games.debug.GiantBombDebugCommands;
import com.github.ykrasik.indexter.games.debug.MetacriticDebugCommands;
import com.github.ykrasik.indexter.games.debug.PreferencesDebugCommands;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.metacritic.client.MetacriticGameInfoClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DebugBeanConfiguration {
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
    public DataServiceDebugCommands dataServiceDebugCommands(GameDataService dataService,
                                                             MetacriticGameInfoService infoService) {
        return new DataServiceDebugCommands(dataService, infoService);
    }

    @Bean
    public PreferencesDebugCommands preferencesDebugCommands(GameCollectionPreferencesImpl preferences) {
        return new PreferencesDebugCommands(preferences);
    }
}
