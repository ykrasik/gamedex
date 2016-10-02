package com.github.ykrasik.gamedex.common.module

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 22:57
 */
class CommonModule : AbstractModule() {
    override fun configure() {

    }

    @Provides
    fun config(): Config {
        // TODO: Load all .conf files on classpath
        return ConfigFactory.load()
    }
}