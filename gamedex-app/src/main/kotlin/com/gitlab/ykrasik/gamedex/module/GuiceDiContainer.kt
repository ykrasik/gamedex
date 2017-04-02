package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.common.module.CommonModule
import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.provider.giantbomb.module.GiantBombProviderModule
import com.gitlab.ykrasik.gamedex.provider.igdb.module.IgdbProviderModule
import com.gitlab.ykrasik.gamedex.provider.module.DataProviderModule
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import tornadofx.DIContainer
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 02/04/2017
 * Time: 11:58
 */
class GuiceDiContainer(modules: List<Module>) : DIContainer {
    private val injector = Guice.createInjector(Stage.PRODUCTION, modules)

    override fun <T : Any> getInstance(type: KClass<T>): T = injector.getInstance(type.java)
}

open class DefaultGuiceModuleConfiguration {
    open val common: Module get() = CommonModule()
    open val persistence: Module get() = PersistenceModule()
    open val dataProvider: Module get() = DataProviderModule()
    open val giantBomb: Module get() = GiantBombProviderModule()
    open val igdb: Module get() = IgdbProviderModule()
    open val app: Module get() = AppModule()

    val modules get() = listOf(common, persistence, dataProvider, giantBomb, igdb, app)
}