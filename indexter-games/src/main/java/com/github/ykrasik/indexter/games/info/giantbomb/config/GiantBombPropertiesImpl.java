package com.github.ykrasik.indexter.games.info.giantbomb.config;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.AbstractGameInfoProperties;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombPropertiesImpl extends AbstractGameInfoProperties implements GiantBombProperties {
    private final String applicationKey;
    private final Map<GamePlatform, Integer> platformIdMap;

    public GiantBombPropertiesImpl() {
        final Properties properties = readProperties("giantbomb.properties");
        this.applicationKey = Objects.requireNonNull(properties.getProperty("applicationKey"));
        this.platformIdMap = getPlatformIdMap(properties, "platformIdMap");
    }

    @Override
    public String getApplicationKey() {
        return applicationKey;
    }

    @Override
    public int getPlatformId(GamePlatform gamePlatform) {
        return Objects.requireNonNull(platformIdMap.get(gamePlatform));
    }
}
