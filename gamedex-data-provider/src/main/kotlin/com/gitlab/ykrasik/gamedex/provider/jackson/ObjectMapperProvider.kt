package com.gitlab.ykrasik.gamedex.provider.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
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
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
}