package com.github.ykrasik.indexter.games.info.giantbomb.config;

import com.github.ykrasik.indexter.games.info.Platform;

/**
 * @author Yevgeny Krasik
 */
public interface GiantBombProperties {
    String getApplicationKey();

    int getPlatformId(Platform platform);
}
