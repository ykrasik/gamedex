package com.github.ykrasik.indexter.games.info.metacritic.config;

import com.github.ykrasik.indexter.games.info.AbstractGameInfoProperties;
import com.github.ykrasik.indexter.games.info.Platform;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticPropertiesImpl extends AbstractGameInfoProperties implements MetacriticProperties {
    private final String applicationKey;
    private final Map<Platform, Integer> platformIdMap;

    public MetacriticPropertiesImpl() {
        final Properties properties = readProperties("metacritic.properties");
        this.applicationKey = Objects.requireNonNull(properties.getProperty("applicationKey"));
        this.platformIdMap = getPlatformIdMap(properties, "platformIdMap");
    }

    @Override
    public String getApplicationKey() {
        return applicationKey;
    }

    @Override
    public int getPlatformId(Platform platform) {
        return Objects.requireNonNull(platformIdMap.get(platform));
    }
}
