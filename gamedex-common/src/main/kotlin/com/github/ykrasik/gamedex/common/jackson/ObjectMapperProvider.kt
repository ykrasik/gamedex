package com.github.ykrasik.gamedex.common.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

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

inline fun <reified T : Any> ObjectMapper.readList(src: String): List<T> {
    val type = this.typeFactory.constructCollectionType(List::class.java, T::class.java)
    return this.readValue(src, type)
}