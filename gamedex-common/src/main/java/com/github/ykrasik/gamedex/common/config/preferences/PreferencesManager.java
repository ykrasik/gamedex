package com.github.ykrasik.gamedex.common.config.preferences;

import com.github.ykrasik.gamedex.common.util.StringUtils;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.list.ImmutableList;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.impl.factory.Lists;
import com.gs.collections.impl.factory.Maps;
import lombok.NonNull;

import java.io.File;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * @author Yevgeny Krasik
 */
public class PreferencesManager {
    private final Preferences preferences;

    public PreferencesManager(@NonNull Class<?> clazz) {
        this.preferences = Preferences.userNodeForPackage(clazz);
    }

    public void clear(String name) {
        preferences.remove(name);
    }

    public Opt<String> get(String name) {
        return Opt.ofNullable(preferences.get(name, null));
    }

    public void put(String name, String value) {
        preferences.put(name, value);
    }

    public <T> ImmutableList<T> getList(String name, Function<String, T> deserializer) throws Exception  {
        final Opt<String> valueOpt = get(name);
        return valueOpt.map(value -> StringUtils.parseList(value, deserializer)).getOrElse(Lists.immutable.of());
    }

    public <T> void putList(String name, ImmutableList<T> list, Function<T, String> serializer) {
        final String value = StringUtils.toString(list, serializer);
        preferences.put(name, value);
    }

    public <K, V> ImmutableMap<K, V> getMap(String name, Function<String, K> keyDeserializer, Function<String, V> valueDeserializer) {
        final Opt<String> valueOpt = get(name);
        return valueOpt.map(value -> StringUtils.parseMap(value, keyDeserializer, valueDeserializer)).getOrElse(Maps.immutable.of());
    }

    public <K, V> void putMap(String name, Map<K, V> map, Function<K, String> keySerializer, Function<V, String> valueSerializer) {
        final String value = StringUtils.toString(map, keySerializer, valueSerializer);
        preferences.put(name, value);
    }

    public Opt<File> getFile(String name) {
        return get(name).map(File::new);
    }

    public void setFile(String name, File file) {
        preferences.put(name, file.getAbsolutePath());
    }
}
