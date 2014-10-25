package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.info.provider.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClientImpl;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.config.GiantBombPropertiesImpl;
import com.github.ykrasik.indexter.games.info.provider.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.metacritic.client.MetacriticGameInfoClientImpl;
import com.github.ykrasik.indexter.games.info.provider.metacritic.config.MetacriticProperties;
import com.github.ykrasik.indexter.games.info.provider.metacritic.config.MetacriticPropertiesImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ProviderBeanConfiguration {


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