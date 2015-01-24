package com.github.ykrasik.indexter.games.info.giantbomb.config;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombProperties {
    String getApplicationKey();

    int getPlatformId(GamePlatform gamePlatform);
}
