package com.gitlab.ykrasik.gamedex.provider.igdb.module

import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbConfig
import com.gitlab.ykrasik.gamedex.provider.igdb.IgdbDataProvider
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

/**
 * User: ykrasik
 * Date: 05/02/2017
 * Time: 21:51
 */
class IgdbProviderModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), DataProvider::class.java).addBinding().to(IgdbDataProvider::class.java)
        bind(IgdbConfig::class.java).toInstance(IgdbConfig())
    }
}
