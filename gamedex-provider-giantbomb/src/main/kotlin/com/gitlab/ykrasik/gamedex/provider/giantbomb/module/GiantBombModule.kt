package com.gitlab.ykrasik.gamedex.provider.giantbomb.module

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombConfig
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombProvider
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.typesafe.config.Config
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:56
 */
object GiantBombModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), GameProvider::class.java).addBinding().to(GiantBombProvider::class.java)
    }

    @Provides
    @Singleton
    fun giantBombConfig(config: Config) = GiantBombConfig(config)
}