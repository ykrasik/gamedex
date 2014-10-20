package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.config.AbstractProperties;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class AbstractGameInfoProperties extends AbstractProperties {
    protected Map<GamePlatform, Integer> getPlatformIdMap(Properties properties, String keyName) {
        final String platformIdMapStr = Objects.requireNonNull(properties.getProperty(keyName));
        final Map<GamePlatform, Integer> platformIdMap = new HashMap<>();
        final Iterable<String> keyValuePairs = COMMA_SPLITTER.split(platformIdMapStr);
        for (String keyValuePair : keyValuePairs) {
            final List<String> platformStrAndId = KEY_VALUE_SPLITTER.splitToList(keyValuePair);
            final GamePlatform gamePlatform = GamePlatform.valueOf(platformStrAndId.get(0));
            final Integer platformId = Integer.parseInt(platformStrAndId.get(1));
            platformIdMap.put(gamePlatform, platformId);
        }
        return platformIdMap;
    }
}
