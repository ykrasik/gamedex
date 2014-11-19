package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.IndexterPreloader;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.indexter.games.library.LibraryManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ConfigBeanConfiguration {
    @Autowired
    private IndexterPreloader preloader;

    @Bean
    public GameCollectionConfig gameCollectionConfig() throws IOException {
        preloader.setMessage("Instantiating config...");
        return new GameCollectionConfigImpl();
    }

    @Bean
    public LibraryManager libraryManager(GameCollectionConfig config) {
        preloader.setMessage("Instantiating library manager...");
        return new LibraryManagerImpl(config);
    }
}
