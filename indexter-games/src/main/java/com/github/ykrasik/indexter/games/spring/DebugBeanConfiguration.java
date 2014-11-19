package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.IndexterPreloader;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.debug.DataServiceDebugCommands;
import com.github.ykrasik.indexter.games.debug.GiantBombDebugCommands;
import com.github.ykrasik.indexter.games.debug.MetacriticDebugCommands;
import com.github.ykrasik.indexter.games.debug.ConfigDebugCommands;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.metacritic.client.MetacriticGameInfoClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DebugBeanConfiguration {
    @Autowired
    private IndexterPreloader preloader;

    @Bean
    public MetacriticDebugCommands metacriticDebugCommands(MetacriticGameInfoService service,
                                                           MetacriticGameInfoClient client,
                                                           ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating Metacritic debug commands...");
        return new MetacriticDebugCommands(service, client, objectMapper);
    }

    @Bean
    public GiantBombDebugCommands giantBombDebugCommands(GiantBombGameInfoService service,
                                                         GiantBombGameInfoClient client,
                                                         ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating GiantBomb debug commands...");
        return new GiantBombDebugCommands(service, client, objectMapper);
    }

    @Bean
    public DataServiceDebugCommands dataServiceDebugCommands(GameDataService dataService,
                                                             MetacriticGameInfoService infoService) {
        preloader.setMessage("Instantiating data service debug commands...");
        return new DataServiceDebugCommands(dataService, infoService);
    }

    @Bean
    public ConfigDebugCommands preferencesDebugCommands(GameCollectionConfigImpl preferences) {
        preloader.setMessage("Instantiating config debug commands...");
        return new ConfigDebugCommands(preferences);
    }
}
