package com.github.ykrasik.indexter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
// FIXME: Redundant
public class EnumIdConverter<K, V extends Enum<V> & IdentifiableEnum<K>> {
    private final Map<K, V> map = new HashMap<>();

    public EnumIdConverter(Class<V> valueType) {
        for (V v : valueType.getEnumConstants()) {
            map.put(v.getCode(), v);
        }
    }

    public V get(K key) {
        return map.get(key);
    }
}
