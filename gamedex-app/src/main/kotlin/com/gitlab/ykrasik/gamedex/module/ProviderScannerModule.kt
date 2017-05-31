package com.gitlab.ykrasik.gamedex.module

import com.google.inject.AbstractModule
import org.reflections.Reflections


/**
 * User: ykrasik
 * Date: 30/05/2017
 * Time: 22:14
 */
object ProviderScannerModule : AbstractModule() {
    override fun configure() {
        val providerModules = scanProviderModules()
        providerModules.forEach { install(it) }
    }

    private fun scanProviderModules(): List<AbstractModule> {
        val classes = Reflections("com.gitlab.ykrasik.gamedex.provider").getSubTypesOf(AbstractModule::class.java)
        return classes.map { it.kotlin.objectInstance!! }
    }
}