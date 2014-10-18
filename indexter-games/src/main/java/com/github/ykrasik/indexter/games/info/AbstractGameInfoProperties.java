package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.config.AbstractProperties;

import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class AbstractGameInfoProperties extends AbstractProperties {
    protected Map<Platform, Integer> getPlatformIdMap(Properties properties, String keyName) {
        final String platformIdMapStr = Objects.requireNonNull(properties.getProperty(keyName));
        final Map<Platform, Integer> platformIdMap = new HashMap<>();
        final Iterable<String> keyValuePairs = COMMA_SPLITTER.split(platformIdMapStr);
        for (String keyValuePair : keyValuePairs) {
            final List<String> platformStrAndId = KEY_VALUE_SPLITTER.splitToList(keyValuePair);
            final Platform platform = Platform.valueOf(platformStrAndId.get(0));
            final Integer platformId = Integer.parseInt(platformStrAndId.get(1));
            platformIdMap.put(platform, platformId);
        }
        return platformIdMap;
    }
}
