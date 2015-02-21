package com.github.ykrasik.gamedex.provider.metacritic.config;

import com.github.ykrasik.gamedex.common.config.properties.PropertiesParser;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.gs.collections.api.map.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticPropertiesImpl implements MetacriticProperties {
    @Getter private final String applicationKey;
    private final ImmutableMap<GamePlatform, Integer> platformIdMap;

    public MetacriticPropertiesImpl() {
        final PropertiesParser parser = new PropertiesParser("metacritic.properties", MetacriticPropertiesImpl.class);
        this.applicationKey = parser.getString("gameDex.info.metacritic.applicationKey");
        this.platformIdMap = parser.parseMap("gameDex.info.metacritic.platformIdMap", GamePlatform::valueOf, Integer::parseInt);
    }

    @Override
    public int getPlatformId(GamePlatform gamePlatform) {
        @NonNull final Integer id = platformIdMap.get(gamePlatform);
        return id;
    }
}
