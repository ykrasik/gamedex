package com.gitlab.ykrasik.gamedex.provider.giantbomb.module

import com.github.ykrasik.gamedex.common.getObjectMap
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombConfig
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombDataProvider
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
class GiantBombProviderModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), DataProvider::class.java).addBinding().to(GiantBombDataProvider::class.java)
    }

    @Provides
    @Singleton
    fun giantBombConfig(config: Config): GiantBombConfig =
        config.getConfig("gameDex.provider.giantBomb").let { config ->
            GiantBombConfig(
                applicationKey = config.getString("applicationKey"),
                platforms = config.getObjectMap("platforms", { GamePlatform.valueOf(it) }, { it as Int })
            )
        }
}