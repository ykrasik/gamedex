package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import com.github.ykrasik.indexter.games.persistence.PersistenceServiceImpl;
import com.github.ykrasik.indexter.games.persistence.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.persistence.config.PersistencePropertiesImpl;
import com.github.ykrasik.indexter.games.persistence.translator.GameEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.GameEntityTranslatorImpl;
import com.github.ykrasik.indexter.games.persistence.translator.LibraryEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.LibraryEntityTranslatorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class DataBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public PersistenceService persistenceService(PersistenceProperties properties,
                                                 GameEntityTranslator gameTranslator,
                                                 LibraryEntityTranslator libraryTranslator) {
        preloader.setMessage("Instantiating data service...");
        return new PersistenceServiceImpl(properties, gameTranslator, libraryTranslator);
    }

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

    @Bean
    public LibraryEntityTranslator libraryEntityTranslator() {
        preloader.setMessage("Instantiating library entity translator...");
        return new LibraryEntityTranslatorImpl();
    }
}
