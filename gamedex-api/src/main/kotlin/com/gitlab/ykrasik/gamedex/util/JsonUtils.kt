/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 09:21
 */
val objectMapper: ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule())
    .registerModule(JodaModule())
    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
    .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)

fun Any.toJsonStr(): String = objectMapper.writeValueAsString(this)

inline fun <reified T : Any> String.fromJson(): T = fromJson(T::class)
fun <T : Any> String.fromJson(klass: KClass<T>): T = objectMapper.readValue(this, klass.java)
inline fun <reified T : Any> ByteArray.fromJson(): T = objectMapper.readValue(this, T::class.java)
inline fun <reified T : Any> File.readJson(): T = readJson(T::class)
fun <T : Any> File.readJson(klass: KClass<T>): T = objectMapper.readValue(this, klass.java)

fun File.writeJson(data: Any) = objectMapper.writerWithDefaultPrettyPrinter().writeValue(this, data)

inline fun <reified T : Any> String.listFromJson(): List<T> {
    val type = objectMapper.typeFactory.constructCollectionType(List::class.java, T::class.java)
    return objectMapper.readValue(this, type)
}

inline fun <reified T : Any> ByteArray.listFromJson(): List<T> {
    val type = objectMapper.typeFactory.constructCollectionType(List::class.java, T::class.java)
    return objectMapper.readValue(this, type)
}

inline fun <reified K : Any, reified V : Any> String.mapFromJson(): Map<K, V> {
    val type = objectMapper.typeFactory.constructMapType(Map::class.java, K::class.java, V::class.java)
    return objectMapper.readValue(this, type)
}