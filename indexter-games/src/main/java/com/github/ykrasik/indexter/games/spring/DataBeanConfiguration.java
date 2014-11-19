package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.IndexterPreloader;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.data.GameDataServiceImpl;
import com.github.ykrasik.indexter.games.data.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.data.config.PersistencePropertiesImpl;
import com.github.ykrasik.indexter.games.data.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.data.translator.GameEntityTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DataBeanConfiguration {
    @Autowired
    private IndexterPreloader preloader;

    @Bean
    public GameDataService gameDataService(PersistenceProperties properties, GameEntityTranslator translator) {
        preloader.setMessage("Instantiating data service...");
        return new GameDataServiceImpl(properties, translator);
    }

    // FIXME: Why is this called dataService and persistenceProperties?
    @Bean
    public PersistenceProperties persistenceProperties() {
        preloader.setMessage("Instantiating persistence service...");
        return new PersistencePropertiesImpl();
    }

    @Bean
    public GameEntityTranslator gameEntityTranslator() {
        preloader.setMessage("Instantiating game entity translator...");
        return new GameEntityTranslatorImpl();
    }
}
