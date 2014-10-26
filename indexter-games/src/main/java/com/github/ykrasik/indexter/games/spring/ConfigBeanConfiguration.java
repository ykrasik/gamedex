package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.config.GameCollectionPreferences;
import com.github.ykrasik.indexter.games.config.GameCollectionPreferencesImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class ConfigBeanConfiguration {
    @Bean
    public GameCollectionPreferences gameCollectionPreferences() {
        return new GameCollectionPreferencesImpl();
    }
}
