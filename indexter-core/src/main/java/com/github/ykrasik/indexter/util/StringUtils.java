package com.github.ykrasik.indexter.util;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class StringUtils {
    private static final char LIST_DELIMITER = ',';
    private static final Splitter LIST_SPLITTER = Splitter.on(LIST_DELIMITER).trimResults().omitEmptyStrings();
    private static final Joiner LIST_JOINER = Joiner.on(LIST_DELIMITER).skipNulls();

    private static final char KEY_VALUE_DELIMITER = ':';
    private static final Splitter KEY_VALUE_SPLITTER = Splitter.on(KEY_VALUE_DELIMITER).trimResults().omitEmptyStrings();
    private static final Joiner KEY_VALUE_JOINER = Joiner.on(KEY_VALUE_DELIMITER).skipNulls();

    private StringUtils() {
    }

    public static <T> List<T> parseList(String str, Function<String, T> deserializer) {
        final List<String> splitList = LIST_SPLITTER.splitToList(str);
        return ListUtils.map(splitList, deserializer);
    }

    public static <K, V> Map<K, V> parseMap(String str, Function<String, K> keyDeserializer, Function<String, V> valueDeserializer) {
        final Map<K, V> map = new HashMap<>();
        final Iterable<String> keyValuePairs = LIST_SPLITTER.split(str);
        for (String keyValuePairStr : keyValuePairs) {
            final List<String> keyValuePair = KEY_VALUE_SPLITTER.splitToList(keyValuePairStr);
            if (keyValuePair.size() != 2) {
                throw new IndexterException("Invalid key value pair for map: '%s'", keyValuePairStr);
            }
            final K key = keyDeserializer.apply(keyValuePair.get(0));
            final V value = valueDeserializer.apply(keyValuePair.get(1));
            map.put(key, value);
        }
        return map;
    }

    public static <T> String toList(List<T> list, Function<T, String> serializer) {
        final List<String> stringList = ListUtils.map(list, serializer);
        return LIST_JOINER.join(stringList);
    }

    public static <K, V> String toMap(Map<K, V> map, Function<String, K> keySerializer, Function<String, V> valueSerializer) {
        return null;
    }
}
