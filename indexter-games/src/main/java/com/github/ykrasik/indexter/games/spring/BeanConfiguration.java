package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.data.GameDataServiceImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClientImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class BeanConfiguration {
    @Bean
    public GameDataService gameDataService() {
        return new GameDataServiceImpl();
    }

    @Bean
    public MetacriticGameInfoService metacriticGameInfoService(MetacriticGameInfoClient client,
                                                               ObjectMapper objectMapper) {
        return new MetacriticGameInfoService(client, objectMapper);
    }

    @Bean
    public MetacriticGameInfoClient metacriticGameInfoClient() {
        return new MetacriticGameInfoClientImpl();
    }

    // TODO: Interface.
    @Bean
    public GiantBombGameInfoService giantBombGameInformationProvider() {
        return new GiantBombGameInfoService();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}