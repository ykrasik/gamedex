package com.github.ykrasik.gamedex.provider.giantbomb.config;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombProperties {
    String getApplicationKey();

    int getPlatformId(GamePlatform gamePlatform);
}
