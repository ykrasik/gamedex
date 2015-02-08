package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.persistence.PersistenceService;
import com.github.ykrasik.indexter.games.persistence.PersistenceServiceImpl;
import com.github.ykrasik.indexter.games.persistence.config.PersistenceProperties;
import com.github.ykrasik.indexter.games.persistence.config.PersistencePropertiesImpl;
import com.github.ykrasik.indexter.games.persistence.translator.exclude.ExcludedPathEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.exclude.ExcludedPathEntityTranslatorImpl;
import com.github.ykrasik.indexter.games.persistence.translator.game.GameEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.game.GameEntityTranslatorImpl;
import com.github.ykrasik.indexter.games.persistence.translator.genre.GenreEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.genre.GenreEntityTranslatorImpl;
import com.github.ykrasik.indexter.games.persistence.translator.library.LibraryEntityTranslator;
import com.github.ykrasik.indexter.games.persistence.translator.library.LibraryEntityTranslatorImpl;
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
                                                 GenreEntityTranslator genreTranslator,
                                                 LibraryEntityTranslator libraryTranslator,
                                                 ExcludedPathEntityTranslator excludedPathTranslator) {
        preloader.setMessage("Loading persistence service...");
        return new PersistenceServiceImpl(properties, gameTranslator, genreTranslator, libraryTranslator, excludedPathTranslator);
    }

    @Bean
    public PersistenceProperties persistenceProperties() {
        preloader.setMessage("Loading persistence properties...");
        return new PersistencePropertiesImpl();
    }

    @Bean
    public GameEntityTranslator gameEntityTranslator() {
        return new GameEntityTranslatorImpl();
    }

    @Bean
    public GenreEntityTranslator genreEntityTranslator() {
        return new GenreEntityTranslatorImpl();
    }

    @Bean
    public LibraryEntityTranslator libraryEntityTranslator() {
        return new LibraryEntityTranslatorImpl();
    }

    @Bean
    public ExcludedPathEntityTranslator excludedPathEntityTranslator() {
        return new ExcludedPathEntityTranslatorImpl();
    }
}
