package com.github.ykrasik.gamedex.common

import com.typesafe.config.Config

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 21:56
 */

fun <K, V> Config.getObjectMap(path: String, keyParser: (String) -> K, valueParser: (Any) -> V): Map<K, V> {
    return getObject(path).entries.associateBy(
        keySelector = { keyParser(it.key) },
        valueTransform = { valueParser(it.value.unwrapped()) }
    )
}