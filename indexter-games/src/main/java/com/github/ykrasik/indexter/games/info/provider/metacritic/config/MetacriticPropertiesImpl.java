package com.github.ykrasik.indexter.games.info.provider.metacritic.config;

import com.github.ykrasik.indexter.config.PropertiesParser;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.util.Map;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticPropertiesImpl implements MetacriticProperties {
    private final String applicationKey;
    private final Map<GamePlatform, Integer> platformIdMap;

    public MetacriticPropertiesImpl() {
        final PropertiesParser parser = new PropertiesParser("metacritic.properties", MetacriticPropertiesImpl.class);
        this.applicationKey = parser.getString("applicationKey");
        this.platformIdMap = parser.parseMap("platformIdMap", GamePlatform::valueOf, Integer::parseInt);
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
