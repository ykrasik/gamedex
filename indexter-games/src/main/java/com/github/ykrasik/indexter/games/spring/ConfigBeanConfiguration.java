package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.config.GameCollectionConfigImpl;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.indexter.games.library.LibraryManagerImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ConfigBeanConfiguration {
    @Bean
    public GameCollectionConfig gameCollectionConfig(ObjectMapper mapper) throws IOException {
        return new GameCollectionConfigImpl(mapper);
    }

    @Bean
    public LibraryManager libraryManager(GameCollectionConfig config) {
        return new LibraryManagerImpl(config);
    }
}
