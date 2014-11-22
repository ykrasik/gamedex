package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.IndexterPreloader;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ProviderBeanConfiguration {
    @Autowired
    private IndexterPreloader preloader;

    @Qualifier("metacriticInfoService")
    @Bean
    public MetacriticGameInfoService metacriticGameInfoService(MetacriticGameInfoClient client,
                                                               MetacriticProperties properties,
                                                               ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating Metacritic game info service...");
        return new MetacriticGameInfoService(client, properties, objectMapper);
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
    public GiantBombGameInfoService giantBombGameInfoService(GiantBombGameInfoClient client,
                                                             GiantBombProperties properties,
                                                             ObjectMapper objectMapper) {
        preloader.setMessage("Instantiating GiantBomb game info service...");
        return new GiantBombGameInfoService(client, properties, objectMapper);
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