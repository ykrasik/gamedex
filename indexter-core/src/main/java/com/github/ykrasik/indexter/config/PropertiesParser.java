package com.github.ykrasik.indexter.config;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.google.common.base.Splitter;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Yevgeny Krasik
 */
public class PropertiesParser {
    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
    private static final Splitter KEY_VALUE_SPLITTER = Splitter.on(':').trimResults().omitEmptyStrings();

    private final Properties properties;

    public PropertiesParser(String url) {
        this(url, PropertiesParser.class);
    }

    public PropertiesParser(String url, Class<?> clazz) {
        this.properties = readProperties(Objects.requireNonNull(url), clazz);
    }

    private Properties readProperties(String url, Class<?> clazz) {
        try {
            final InputStream propertiesStream = Objects.requireNonNull(clazz.getResourceAsStream(url));
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties;
        } catch (Exception e) {
            throw new IndexterException(e);
        }
    }

    public String getString(String name) {
        return Objects.requireNonNull(properties.getProperty(name));
    }

    public long getLong(String name) {
        return Long.parseLong(getString(name));
    }

    public Duration getMillis(String name) {
        return Duration.ofMillis(getLong(name));
    }

    public Duration getSeconds(String name) {
        return Duration.ofSeconds(getLong(name));
    }

    public Duration getMinutes(String name) {
        return Duration.ofMinutes(getLong(name));
    }

    public <T> List<T> parseList(String name, Function<String, T> f) {
        final List<String> splitList = COMMA_SPLITTER.splitToList(getString(name));
        return splitList.stream().map(f::apply).collect(Collectors.toList());
    }

    public <K, V> Map<K, V> parseMap(String name, Function<String, K> keyFunction, Function<String, V> valueFunction) {
        final Map<K, V> map = new HashMap<>();
        final Iterable<String> keyValuePairs = COMMA_SPLITTER.split(getString(name));
        for (String keyValuePairStr : keyValuePairs) {
            final List<String> keyValuePair = KEY_VALUE_SPLITTER.splitToList(keyValuePairStr);
            if (keyValuePair.size() != 2) {
                throw new IndexterException("Invalid key value pair for map: '%s'", keyValuePairStr);
            }
            final K key = keyFunction.apply(keyValuePair.get(0));
            final V value = valueFunction.apply(keyValuePair.get(1));
            map.put(key, value);
        }
        return map;
    }
}
