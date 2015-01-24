package com.github.ykrasik.indexter.games.info.metacritic.config;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

/**
 * @author Yevgeny Krasik
 */
public interface MetacriticProperties {
    String getApplicationKey();

    int getPlatformId(GamePlatform gamePlatform);
}
