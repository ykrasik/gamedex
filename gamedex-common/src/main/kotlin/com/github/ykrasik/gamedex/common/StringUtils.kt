package com.github.ykrasik.gamedex.common

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 20:23
 */

// FIXME: Doesn't belong here.
private val NOT_AVAILABLE = "NA"

fun <T> String.parseList(deserializer: (String) -> T): List<T> = parseList(',', deserializer)
fun <T> String.parseList(delimiter: Char, deserializer: (String) -> T): List<T> {
    return this.splitToSequence(delimiter).map { deserializer(it) }.toList()
}

fun <K, V> String.parseMap(entryDelimiter: Char = ',',
                           keyValueDelimiter: Char = ':',
                           keyDeserializer: (String) -> K,
                           valueDeserializer: (String) -> V): Map<K, V> {
    return this.splitToSequence(entryDelimiter).map {
        val keyValue = it.split(keyValueDelimiter)
        require(keyValue.size == 2) { "Invalid key-value pair in map: $it" }
        val key = keyDeserializer(keyValue[0])
        val value = valueDeserializer(keyValue[1])
        key to value
    }.toMap()
}

fun <T> T?.toString(default: String): String = this?.let { it.toString() } ?: default

// FIXME: Doesn't belong here
fun <T> T?.toStringOrUnavailable(): String = toString(NOT_AVAILABLE)

// FIXME: Doesn't belong here.
val String.isUnavailable: Boolean get() = this == NOT_AVAILABLE