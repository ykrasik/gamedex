package com.github.ykrasik.indexter.games.info.provider.giantbomb.config;

import com.github.ykrasik.indexter.config.properties.PropertiesParser;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.util.Map;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombPropertiesImpl implements GiantBombProperties {
    private final String applicationKey;
    private final Map<GamePlatform, Integer> platformIdMap;

    public GiantBombPropertiesImpl() {
        final PropertiesParser parser = new PropertiesParser("giantbomb.properties", GiantBombPropertiesImpl.class);
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
