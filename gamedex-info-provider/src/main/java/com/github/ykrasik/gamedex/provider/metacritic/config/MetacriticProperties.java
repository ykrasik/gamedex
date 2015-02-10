package com.github.ykrasik.gamedex.provider.metacritic.config;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;

/**
 * @author Yevgeny Krasik
 */
public interface MetacriticProperties {
    String getApplicationKey();

    int getPlatformId(GamePlatform gamePlatform);
}
