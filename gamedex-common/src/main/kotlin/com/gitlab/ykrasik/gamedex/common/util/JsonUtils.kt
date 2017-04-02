package com.gitlab.ykrasik.gamedex.common.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 09:21
 */
// TODO: Are global variables like this lazy by default?
val objectMapper: ObjectMapper by lazy {
    ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
}

fun Any.toJsonStr(): String = objectMapper.writeValueAsString(this)

inline fun <reified T : Any> String.fromJson(): T = objectMapper.readValue(this, T::class.java)
inline fun <reified T : Any> ByteArray.fromJson(): T = objectMapper.readValue(this, T::class.java)
inline fun <reified T : Any> File.readJson(): T = objectMapper.readValue(this, T::class.java)

fun File.writeJson(data: Any) = objectMapper.writeValue(this, data)

inline fun <reified T : Any> String.listFromJson(): List<T> {
    val type = objectMapper.typeFactory.constructCollectionType(List::class.java, T::class.java)
    return objectMapper.readValue(this, type)
}
inline fun <reified T : Any> ByteArray.listFromJson(): List<T> {
    val type = objectMapper.typeFactory.constructCollectionType(List::class.java, T::class.java)
    return objectMapper.readValue(this, type)
}

inline fun <reified K : Any, reified V: Any> String.mapFromJson(): Map<K, V> {
    val type = objectMapper.typeFactory.constructMapType(Map::class.java, K::class.java, V::class.java)
    return objectMapper.readValue(this, type)
}