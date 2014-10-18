package com.github.ykrasik.indexter.games.info.metacritic.config;

import com.github.ykrasik.indexter.games.info.Platform;

/**
 * @author Yevgeny Krasik
 */
public interface MetacriticProperties {
    String getApplicationKey();

    int getPlatformId(Platform platform);
}
