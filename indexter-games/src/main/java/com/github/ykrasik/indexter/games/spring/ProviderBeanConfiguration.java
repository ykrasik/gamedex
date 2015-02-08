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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ProviderBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public MetacriticGameInfoService metacriticGameInfoService(MetacriticGameInfoClient client,
                                                               MetacriticProperties properties,
                                                               ObjectMapper objectMapper) {
        preloader.setMessage("Loading Metacritic service...");
        return new MetacriticGameInfoService(client, properties, objectMapper);
    }

    @Bean
    public MetacriticGameInfoClient metacriticGameInfoClient(MetacriticProperties properties) {
        preloader.setMessage("Loading Metacritic client...");
        return new MetacriticGameInfoClientImpl(properties);
    }

    @Bean
    public MetacriticProperties metacriticProperties() {
        preloader.setMessage("Loading Metacritic properties...");
        return new MetacriticPropertiesImpl();
    }

    @Bean
    public GiantBombGameInfoService giantBombGameInfoService(GiantBombGameInfoClient client,
                                                             GiantBombProperties properties,
                                                             ObjectMapper objectMapper) {
        preloader.setMessage("Loading GiantBomb service...");
        return new GiantBombGameInfoService(client, properties, objectMapper);
    }

    @Bean
    public GiantBombGameInfoClient giantBombGameInfoClient(GiantBombProperties properties) {
        preloader.setMessage("Loading GiantBomb client...");
        return new GiantBombGameInfoClientImpl(properties);
    }

    @Bean
    public GiantBombProperties giantBombProperties() {
        preloader.setMessage("Loading GiantBomb properties...");
        return new GiantBombPropertiesImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        preloader.setMessage("Loading JSON Object mapper...");
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}