package com.github.ykrasik.gamedex.common.config.properties;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.common.util.StringUtils;
import lombok.NonNull;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

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

    public <T> List<T> parseList(String name, Function<String, T> f) {
        return StringUtils.parseList(getString(name), f);
    }

    public <K, V> Map<K, V> parseMap(String name, Function<String, K> keyFunction, Function<String, V> valueFunction) {
        return StringUtils.parseMap(getString(name), keyFunction, valueFunction);
    }
}
