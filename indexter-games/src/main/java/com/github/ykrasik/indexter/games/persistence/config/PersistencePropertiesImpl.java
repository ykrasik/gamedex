package com.github.ykrasik.indexter.games.persistence.config;

import com.github.ykrasik.indexter.config.properties.PropertiesParser;

import java.time.Duration;

/**
 * @author Yevgeny Krasik
 */
public class PersistencePropertiesImpl implements PersistenceProperties {
    private final String dbUrl;
    private final Duration connectionCheckInterval;

    public PersistencePropertiesImpl() {
        final PropertiesParser parser = new PropertiesParser("data.properties", PersistencePropertiesImpl.class);
        this.dbUrl = parser.getString("dbUrl");
        this.connectionCheckInterval = parser.getMinutes("connectionCheckInternalMinutes");
    }

    @Override
    public String getDbUrl() {
        return dbUrl;
    }

    @Override
    public Duration getConnectionCheckInterval() {
        return connectionCheckInterval;
    }
}
