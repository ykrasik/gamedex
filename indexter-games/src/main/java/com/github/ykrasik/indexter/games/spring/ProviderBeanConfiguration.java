package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoServiceImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClientImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombPropertiesImpl;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoServiceImpl;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClientImpl;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticPropertiesImpl;
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
    @Qualifier("metacriticInfoService")
    @Bean
    public MetacriticGameInfoServiceImpl metacriticGameInfoService(MetacriticGameInfoClient client,
                                                               MetacriticProperties properties,
                                                               ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating Metacritic game info service...");
        return new MetacriticGameInfoServiceImpl(client, properties, objectMapper);
    }

    @Bean
    public MetacriticGameInfoClient metacriticGameInfoClient(MetacriticProperties properties) {
        preloader.setMessage("Instantiating Metacritic game info client...");
        return new MetacriticGameInfoClientImpl(properties);
    }

    @Bean
    public MetacriticProperties metacriticProperties() {
        preloader.setMessage("Instantiating Metacritic game info properties...");
        return new MetacriticPropertiesImpl();
    }

    @Qualifier("giantBombInfoService")
    @Bean
    public GiantBombGameInfoServiceImpl giantBombGameInfoService(GiantBombGameInfoClient client,
                                                             GiantBombProperties properties,
                                                             ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating GiantBomb game info service...");
        return new GiantBombGameInfoServiceImpl(client, properties, objectMapper);
    }

    @Bean
    public GiantBombGameInfoClient giantBombGameInfoClient(GiantBombProperties properties) {
        preloader.setMessage("Instantiating GiantBomb game info client...");
        return new GiantBombGameInfoClientImpl(properties);
    }

    @Bean
    public GiantBombProperties giantBombProperties() {
        preloader.setMessage("Instantiating GiantBomb game info properties...");
        return new GiantBombPropertiesImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        preloader.setMessage("Instantiating JSON Object mapper...");
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}