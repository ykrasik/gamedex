package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.util.appConfig
import com.google.inject.AbstractModule
import com.typesafe.config.Config

/**
 * User: ykrasik
 * Date: 22/04/2017
 * Time: 15:36
 */
object ConfigModule : AbstractModule() {
    override fun configure() {
        bind(Config::class.java).toInstance(appConfig)
    }
}