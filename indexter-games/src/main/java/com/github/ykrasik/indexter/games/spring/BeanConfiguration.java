package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClientImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombPropertiesImpl;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClientImpl;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticPropertiesImpl;
import com.github.ykrasik.indexter.games.persistence.GameDataService;
import com.github.ykrasik.indexter.games.persistence.GameDataServiceImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
    public BeanPostProcessor gameDataListenerBeanProcessor(GameDataService dataService) {
        return new GameDataListenerBeanProcessor(dataService);
    }

    // FIXME: Find a solution to this.
    @Primary
    @Bean
    public MetacriticGameInfoService metacriticGameInfoService(MetacriticGameInfoClient client,
                                                               MetacriticProperties properties,
                                                               ObjectMapper objectMapper) {
        return new MetacriticGameInfoService(client, properties, objectMapper);
    }

    @Bean
    public MetacriticGameInfoClient metacriticGameInfoClient(MetacriticProperties properties) {
        return new MetacriticGameInfoClientImpl(properties);
    }

    @Bean
    public MetacriticProperties metacriticProperties() {
        return new MetacriticPropertiesImpl();
    }

    @Bean
    public GiantBombGameInfoService giantBombGameInfoService(GiantBombGameInfoClient client,
                                                             GiantBombProperties properties,
                                                             ObjectMapper objectMapper) {
        return new GiantBombGameInfoService(client, properties, objectMapper);
    }

    @Bean
    public GiantBombGameInfoClient giantBombGameInfoClient(GiantBombProperties properties) {
        return new GiantBombGameInfoClientImpl(properties);
    }

    @Bean
    public GiantBombProperties giantBombProperties() {
        return new GiantBombPropertiesImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}