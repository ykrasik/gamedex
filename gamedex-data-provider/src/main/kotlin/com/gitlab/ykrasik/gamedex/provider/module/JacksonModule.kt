package com.gitlab.ykrasik.gamedex.provider.module

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.AbstractModule
import com.google.inject.Provides

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 10:22
 */
class JacksonModule : AbstractModule() {
    override fun configure() {
    }

    @Provides
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
}