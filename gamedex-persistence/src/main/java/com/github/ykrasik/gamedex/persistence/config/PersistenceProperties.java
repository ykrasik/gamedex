package com.github.ykrasik.gamedex.persistence.config;

import java.time.Duration;

/**
 * @author Yevgeny Krasik
 */
public interface PersistenceProperties {
    String getDbUrl();
    Duration getConnectionCheckInterval();
}
