package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.persistence.module.PersistenceModule
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
class GuiceDiContainer(modules: List<Module> = GuiceDiContainer.defaultModules) : DIContainer {
    private val injector = Guice.createInjector(Stage.PRODUCTION, modules)

    override fun <T : Any> getInstance(type: KClass<T>): T = injector.getInstance(type.java)

    companion object {
        val defaultModules = listOf(
            AppModule, ConfigModule, PersistenceModule, ProviderScannerModule
        )
    }
}