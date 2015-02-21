package com.github.ykrasik.gamedex.provider.spring;

import com.github.ykrasik.gamedex.common.spring.AbstractBeanConfiguration;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.gamedex.provider.giantbomb.GiantBombGameInfoProvider;
import com.github.ykrasik.gamedex.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.gamedex.provider.giantbomb.client.GiantBombGameInfoClientImpl;
import com.github.ykrasik.gamedex.provider.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.gamedex.provider.giantbomb.config.GiantBombPropertiesImpl;
import com.github.ykrasik.gamedex.provider.giantbomb.debug.GiantBombDebugCommands;
import com.github.ykrasik.gamedex.provider.metacritic.MetacriticGameInfoProvider;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClientImpl;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticProperties;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticPropertiesImpl;
import com.github.ykrasik.gamedex.provider.metacritic.debug.MetacriticDebugCommands;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ProviderBeanConfiguration extends AbstractBeanConfiguration {
    @Qualifier("metacriticGameInfoProvider")
    @Bean
    public GameInfoProvider metacriticGameInfoProvider(MetacriticGameInfoClient client,
                                                       MetacriticProperties properties,
                                                       ObjectMapper objectMapper) {
        return new MetacriticGameInfoProvider(client, properties, objectMapper);
    }

    @Bean
    public MetacriticGameInfoClient metacriticGameInfoClient(MetacriticProperties properties) {
        preloader.message("Loading Metacritic...");
        return new MetacriticGameInfoClientImpl(properties);
    }

    @Bean
    public MetacriticProperties metacriticProperties() {
        return new MetacriticPropertiesImpl();
    }

    @Bean
    public MetacriticDebugCommands metacriticDebugCommands(MetacriticGameInfoProvider service,
                                                           MetacriticGameInfoClient client,
                                                           ObjectMapper objectMapper) {
        return new MetacriticDebugCommands(service, client, objectMapper);
    }

    @Qualifier("giantBombGameInfoProvider")
    @Bean
    public GameInfoProvider giantBombGameInfoProvider(GiantBombGameInfoClient client,
                                                      GiantBombProperties properties,
                                                      ObjectMapper objectMapper) {
        return new GiantBombGameInfoProvider(client, properties, objectMapper);
    }

    @Bean
    public GiantBombGameInfoClient giantBombGameInfoClient(GiantBombProperties properties) {
        preloader.message("Loading GiantBomb...");
        return new GiantBombGameInfoClientImpl(properties);
    }

    @Bean
    public GiantBombProperties giantBombProperties() {
        return new GiantBombPropertiesImpl();
    }

    @Bean
    public GiantBombDebugCommands giantBombDebugCommands(GiantBombGameInfoProvider service,
                                                         GiantBombGameInfoClient client,
                                                         ObjectMapper objectMapper) {
        return new GiantBombDebugCommands(service, client, objectMapper);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}