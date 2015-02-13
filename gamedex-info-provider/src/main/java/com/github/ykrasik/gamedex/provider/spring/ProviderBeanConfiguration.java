package com.github.ykrasik.gamedex.provider.spring;

import com.github.ykrasik.gamedex.common.spring.AbstractBeanConfiguration;
import com.github.ykrasik.gamedex.provider.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.gamedex.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.gamedex.provider.giantbomb.client.GiantBombGameInfoClientImpl;
import com.github.ykrasik.gamedex.provider.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.gamedex.provider.giantbomb.config.GiantBombPropertiesImpl;
import com.github.ykrasik.gamedex.provider.giantbomb.debug.GiantBombDebugCommands;
import com.github.ykrasik.gamedex.provider.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClientImpl;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticProperties;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticPropertiesImpl;
import com.github.ykrasik.gamedex.provider.metacritic.debug.MetacriticDebugCommands;
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
        preloader.info("Loading Metacritic service...");
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
    public MetacriticDebugCommands metacriticDebugCommands(MetacriticGameInfoService service,
                                                           MetacriticGameInfoClient client,
                                                           ObjectMapper objectMapper) {
        return new MetacriticDebugCommands(service, client, objectMapper);
    }

    @Bean
    public GiantBombGameInfoService giantBombGameInfoService(GiantBombGameInfoClient client,
                                                             GiantBombProperties properties,
                                                             ObjectMapper objectMapper) {
        preloader.info("Loading GiantBomb service...");
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
    public GiantBombDebugCommands giantBombDebugCommands(GiantBombGameInfoService service,
                                                         GiantBombGameInfoClient client,
                                                         ObjectMapper objectMapper) {
        return new GiantBombDebugCommands(service, client, objectMapper);
    }

    @Bean
    public ObjectMapper objectMapper() {
        preloader.info("Loading JSON Object mapper...");
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}