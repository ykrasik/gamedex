package com.github.ykrasik.gamedex.common.config.properties;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.map.ImmutableMap;
import lombok.NonNull;

import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Yevgeny Krasik
 */
public class PropertiesParser {
    private final Properties properties;

    public PropertiesParser(String url) {
        this(url, PropertiesParser.class);
    }

    public PropertiesParser(@NonNull String url, Class<?> clazz) {
        this.properties = readProperties(url, clazz);
    }

    private Properties readProperties(String url, Class<?> clazz) {
        try {
            final InputStream propertiesStream = Objects.requireNonNull(clazz.getResourceAsStream(url), "Resource not found on classpath: " + url);
            final Properties properties = new Properties();
            properties.load(propertiesStream);
            return properties;
        } catch (Exception e) {
            throw new GameDexException(e);
        }
    }

    public String getString(String name) {
        return Objects.requireNonNull(properties.getProperty(name), "Property value not found for: " + name);
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

    public <T> ImmutableList<T> parseList(String name, Function<String, T> f) {
        return StringUtils.parseList(getString(name), f);
    }

    public <K, V> ImmutableMap<K, V> parseMap(String name, Function<String, K> keyFunction, Function<String, V> valueFunction) {
        return StringUtils.parseMap(getString(name), keyFunction, valueFunction);
    }
}
