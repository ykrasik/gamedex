package com.gitlab.ykrasik.gamedex.provider.giantbomb.module

import com.gitlab.ykrasik.gamedex.provider.DataProvider
import com.gitlab.ykrasik.gamedex.provider.giantbomb.*
import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:56
 */
class GiantBombProviderModule : AbstractModule() {
    override fun configure() {
        Multibinder.newSetBinder(binder(), DataProvider::class.java).addBinding().to(GiantBombDataProvider::class.java)
        bind(GiantBombConfig::class.java).to(GiantBombConfigImpl::class.java)
        bind(GiantBombClient::class.java).to(FuelGiantBombClient::class.java)
    }
}