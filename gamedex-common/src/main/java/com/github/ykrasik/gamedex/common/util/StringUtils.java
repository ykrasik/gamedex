package com.github.ykrasik.gamedex.common.util;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.opt.Opt;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.map.MutableMap;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.factory.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Yevgeny Krasik
 */
public final class StringUtils {
    private static final char LIST_DELIMITER = ',';
    private static final Splitter LIST_SPLITTER = Splitter.on(LIST_DELIMITER).trimResults().omitEmptyStrings();
    private static final Joiner LIST_JOINER = Joiner.on(LIST_DELIMITER).skipNulls();
    private static final Joiner LIST_PRETTY_JOINER = Joiner.on(", ").skipNulls();

    private static final String KEY_VALUE_DELIMITER = "::";
    private static final Splitter KEY_VALUE_SPLITTER = Splitter.on(KEY_VALUE_DELIMITER).trimResults().omitEmptyStrings();

    private StringUtils() {
    }

    public static <T> ImmutableList<T> parseList(String str, Function<String, T> deserializer) {
        final ImmutableList<String> list = Lists.immutable.ofAll(LIST_SPLITTER.split(str));
        return list.collect(deserializer);
    }

    public static <K, V> ImmutableMap<K, V> parseMap(String str, Function<String, K> keyDeserializer, Function<String, V> valueDeserializer) {
        final MutableMap<K, V> map = Maps.mutable.of();
        final Iterable<String> keyValuePairs = LIST_SPLITTER.split(str);
        for (String keyValuePairStr : keyValuePairs) {
            final List<String> keyValuePair = KEY_VALUE_SPLITTER.splitToList(keyValuePairStr);
            if (keyValuePair.size() != 2) {
                throw new GameDexException("Invalid key value pair for map: '%s'", keyValuePairStr);
            }
            final K key = keyDeserializer.valueOf(keyValuePair.get(0));
            final V value = valueDeserializer.valueOf(keyValuePair.get(1));
            map.put(key, value);
        }
        return map.toImmutable();
    }

    public static <T> String toString(ImmutableList<T> list, Function<T, String> serializer) {
        final ImmutableList<String> stringList = list.collect(serializer);
        return LIST_JOINER.join(stringList);
    }

    public static <K, V> String toString(Map<K, V> map, Function<K, String> keySerializer, Function<V, String> valueSerializer) {
        final List<String> keyValuePairs = new ArrayList<>(map.size());
        for (Entry<K, V> entry : map.entrySet()) {
            final String key = keySerializer.valueOf(entry.getKey());
            final String value = valueSerializer.valueOf(entry.getValue());
            keyValuePairs.add(String.format("%s %s %s", key, KEY_VALUE_DELIMITER, value));
        }
        return LIST_JOINER.join(keyValuePairs);
    }

    public static <T> String toString(Opt<T> optional, String absent) {
        return optional.map(Object::toString).getOrElse(absent);
    }

    public static <T> String toStringOrUnavailable(Opt<T> optional) {
        return toString(optional, "NA");
    }

    public static String toCsv(Iterable<? extends Object> parts) {
        return LIST_JOINER.join(parts);
    }

    public static String toPrettyCsv(Iterable<? extends Object> parts) {
        return LIST_PRETTY_JOINER.join(parts);
    }
}
