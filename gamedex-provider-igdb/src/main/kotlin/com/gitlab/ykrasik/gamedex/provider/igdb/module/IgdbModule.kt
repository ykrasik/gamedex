package com.gitlab.ykrasik.gamedex.provider.igdb.module

import com.gitlab.ykrasik.gamedex.DataProvider
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbConfig
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbDataProvider
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.typesafe.config.Config
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 05/02/2017
 * Time: 21:51
 */
object IgdbModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), DataProvider::class.java).addBinding().to(IgdbDataProvider::class.java)
    }

    @Provides
    @Singleton
    fun igdbConfig(config: Config) = IgdbConfig(config)
}
