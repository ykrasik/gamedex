package com.gitlab.ykrasik.gamedex.module

import com.gitlab.ykrasik.gamedex.util.ClassPathScanner
import com.google.inject.AbstractModule

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
        val classes = ClassPathScanner.scanSubTypes("com.gitlab.ykrasik.gamedex.provider", AbstractModule::class)
        return classes.map { it.kotlin.objectInstance!! }
    }
}