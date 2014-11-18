package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.indexter.games.library.LibraryManagerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ConfigBeanConfiguration {
    @Bean
    public GameCollectionConfig gameCollectionConfig() throws IOException {
        return new GameCollectionConfigImpl();
    }

    @Bean
    public LibraryManager libraryManager(GameCollectionConfig config) {
        return new LibraryManagerImpl(config);
    }
}
