package com.github.ykrasik.indexter.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
public class EnumIdConverter<K, V extends Enum<V> & IdentifiableEnum<K>> {
    private final Map<K, V> map = new HashMap<>();

    public EnumIdConverter(Class<V> valueType) {
        for (V v : valueType.getEnumConstants()) {
            map.put(v.getKey(), v);
        }
    }

    public V get(K key) {
        return map.get(key);
    }
}
