package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.data.GameDataServiceImpl;
import com.github.ykrasik.indexter.games.data.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.data.config.PersistencePropertiesImpl;
import com.github.ykrasik.indexter.games.data.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.data.translator.GameEntityTranslatorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DataBeanConfiguration {
    @Bean
    public GameDataService gameDataService(PersistenceProperties properties, GameEntityTranslator translator) {
        return new GameDataServiceImpl(properties, translator);
    }

    @Bean
    public PersistenceProperties persistenceProperties() {
        return new PersistencePropertiesImpl();
    }

    @Bean
    public GameEntityTranslator gameEntityTranslator() {
        return new GameEntityTranslatorImpl();
    }
}
