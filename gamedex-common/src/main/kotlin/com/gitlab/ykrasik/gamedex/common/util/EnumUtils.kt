package com.gitlab.ykrasik.gamedex.common.util

import java.util.*

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:55
 */
interface IdentifiableEnum<out K> {
    val key: K
}

class EnumIdConverter<K, V>(valueType: Class<V>) where V : Enum<V>, V : IdentifiableEnum<K> {
    private val map = HashMap<K, V>()

    init {
        for (v in valueType.enumConstants) {
            map.put(v.key, v)
        }
    }

    operator fun get(key: K): V = map[key]!!
}