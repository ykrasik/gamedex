package com.github.ykrasik.gamedex.persistence.config;

import com.github.ykrasik.gamedex.common.config.properties.PropertiesParser;
import lombok.Getter;

import java.time.Duration;

/**
 * @author Yevgeny Krasik
 */
public class PersistencePropertiesImpl implements PersistenceProperties {
    @Getter private final String dbUrl;
    @Getter private final Duration connectionCheckInterval;

    public PersistencePropertiesImpl() {
        final PropertiesParser parser = new PropertiesParser("persistence.properties", PersistencePropertiesImpl.class);
        this.dbUrl = parser.getString("gameDex.persistence.dbUrl");
        this.connectionCheckInterval = parser.getMinutes("gameDex.persistence.connectionCheckInternalMinutes");
    }
}
