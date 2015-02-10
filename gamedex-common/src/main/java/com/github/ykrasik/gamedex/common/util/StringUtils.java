package com.github.ykrasik.gamedex.common.util;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.exception.FunctionThrows;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class StringUtils {
    private static final char LIST_DELIMITER = ',';
    private static final Splitter LIST_SPLITTER = Splitter.on(LIST_DELIMITER).trimResults().omitEmptyStrings();
    private static final Joiner LIST_JOINER = Joiner.on(LIST_DELIMITER).skipNulls();

    private static final String KEY_VALUE_DELIMITER = "::";
    private static final Splitter KEY_VALUE_SPLITTER = Splitter.on(KEY_VALUE_DELIMITER).trimResults().omitEmptyStrings();

    private StringUtils() {
    }

    public static <T> List<T> parseList(String str, FunctionThrows<String, T> deserializer) {
        final List<String> splitList = LIST_SPLITTER.splitToList(str);
        return ListUtils.map(splitList, deserializer);
    }

    public static <K, V> Map<K, V> parseMap(String str, Function<String, K> keyDeserializer, Function<String, V> valueDeserializer) {
        final Map<K, V> map = new HashMap<>();
        final Iterable<String> keyValuePairs = LIST_SPLITTER.split(str);
        for (String keyValuePairStr : keyValuePairs) {
            final List<String> keyValuePair = KEY_VALUE_SPLITTER.splitToList(keyValuePairStr);
            if (keyValuePair.size() != 2) {
                throw new GameDexException("Invalid key value pair for map: '%s'", keyValuePairStr);
            }
            final K key = keyDeserializer.apply(keyValuePair.get(0));
            final V value = valueDeserializer.apply(keyValuePair.get(1));
            map.put(key, value);
        }
        return map;
    }

    public static <T> String toString(List<T> list, FunctionThrows<T, String> serializer) {
        final List<String> stringList = ListUtils.map(list, serializer);
        return LIST_JOINER.join(stringList);
    }

    public static <K, V> String toString(Map<K, V> map, Function<K, String> keySerializer, Function<V, String> valueSerializer) {
        final List<String> keyValuePairs = new ArrayList<>(map.size());
        for (Entry<K, V> entry : map.entrySet()) {
            final String key = keySerializer.apply(entry.getKey());
            final String value = valueSerializer.apply(entry.getValue());
            keyValuePairs.add(String.format("%s %s %s", key, KEY_VALUE_DELIMITER, value));
        }
        return LIST_JOINER.join(keyValuePairs);
    }
}
