package com.github.ykrasik.indexter.config.preferences;

import com.github.ykrasik.indexter.optional.Optionals;
import com.github.ykrasik.indexter.util.StringUtils;
import com.github.ykrasik.indexter.exception.FunctionThrows;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.Preferences;

/**
 * @author Yevgeny Krasik
 */
public class PreferencesManager {
    private final Preferences preferences;

    public PreferencesManager(Class<?> clazz) {
        this.preferences = Preferences.userNodeForPackage(Objects.requireNonNull(clazz));
    }

    public void clear(String name) {
        preferences.remove(name);
    }

    public Optional<String> get(String name) {
        return Optional.ofNullable(preferences.get(name, null));
    }

    public void put(String name, String value) {
        preferences.put(name, value);
    }

    public <T> List<T> getList(String name, FunctionThrows<String, T> deserializer) {
        final Optional<String> optionalValue = get(name);
        return Optionals.flatMapToList(optionalValue, value -> StringUtils.parseList(value, deserializer));
    }

    public <T> void putList(String name, List<T> list, FunctionThrows<T, String> serializer) {
        final String value = StringUtils.toString(list, serializer);
        preferences.put(name, value);
    }

    public <K, V> Map<K, V> getMap(String name, Function<String, K> keyDeserializer, Function<String, V> valueDeserializer) {
        final Optional<String> optionalValue = get(name);
        return Optionals.flatMapToMap(optionalValue, value -> StringUtils.parseMap(value, keyDeserializer, valueDeserializer));
    }

    public <K, V> void putMap(String name, Map<K, V> map, Function<K, String> keySerializer, Function<V, String> valueSerializer) {
        final String value = StringUtils.toString(map, keySerializer, valueSerializer);
        preferences.put(name, value);
    }

    public Optional<File> getFile(String name) {
        return get(name).map(File::new);
    }

    public void setFile(String name, File file) {
        preferences.put(name, file.getAbsolutePath());
    }
}
