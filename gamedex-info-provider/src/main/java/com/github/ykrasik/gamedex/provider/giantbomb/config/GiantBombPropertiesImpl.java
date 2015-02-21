package com.github.ykrasik.gamedex.provider.giantbomb.config;

import com.github.ykrasik.gamedex.common.config.properties.PropertiesParser;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.gs.collections.api.map.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombPropertiesImpl implements GiantBombProperties {
    @Getter private final String applicationKey;
    private final ImmutableMap<GamePlatform, Integer> platformIdMap;

    public GiantBombPropertiesImpl() {
        final PropertiesParser parser = new PropertiesParser("giantbomb.properties", GiantBombPropertiesImpl.class);
        this.applicationKey = parser.getString("gameDex.info.giantBomb.applicationKey");
        this.platformIdMap = parser.parseMap("gameDex.info.giantBomb.platformIdMap", GamePlatform::valueOf, Integer::parseInt);
    }

    @Override
    public int getPlatformId(GamePlatform gamePlatform) {
        @NonNull final Integer id = platformIdMap.get(gamePlatform);
        return id;
    }
}
