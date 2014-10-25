package com.github.ykrasik.indexter.games.data.config;

import java.time.Duration;

/**
 * @author Yevgeny Krasik
 */
public interface PersistenceProperties {
    String getDbUrl();
    Duration getConnectionCheckInterval();
}
