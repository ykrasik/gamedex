/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.preloader

import com.gitlab.ykrasik.gamedex.app.api.Presenters
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.app.api.util.task
import com.gitlab.ykrasik.gamedex.core.log.GamedexLog
import com.gitlab.ykrasik.gamedex.core.log.GamedexLogAppender
import com.gitlab.ykrasik.gamedex.core.module.CoreModule
import com.gitlab.ykrasik.gamedex.core.module.ProviderScannerModule
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Module
import com.google.inject.Stage
import com.google.inject.matcher.Matchers
import com.google.inject.spi.ProvisionListener
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import org.slf4j.bridge.SLF4JBridgeHandler

/**
 * User: ykrasik
 * Date: 13/04/2018
 * Time: 09:06
 */
object Preloader {
    init {
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
        GamedexLogAppender.init()
    }

    fun load(vararg extraModules: Module) = task("Loading...") {
        message1 = "Loading..."

        // While loading, display all log messages in the task
        val subscription = GamedexLog.entries.itemsChannel.subscribe {
            if (it.isNotEmpty()) {
                message1 = it.last().message
            }
        }

        val userConfig = PreloaderUserConfig()

        var componentCount = 0
        val lifecycleModule = LifecycleModule(object : ProvisionListener {
            override fun <T : Any?> onProvision(provision: ProvisionListener.ProvisionInvocation<T>?) {
                componentCount++
                try {
                    progress(componentCount, userConfig.diComponents)
                } catch (_: ClosedSendChannelException) {
                    // This happens when we don't pre-load all required classes during this stage
                    // Some classes get lazily loaded, which will trigger this exception.
                    println("Lazy loading: ${provision!!.binding.key}")  // TODO: Temp
                    // FIXME: Make sure everything is pre-loaded!
                }
            }

        })
        val injector = Guice.createInjector(Stage.PRODUCTION,
            ProviderScannerModule, CoreModule, lifecycleModule, *extraModules
        )

        presenters = injector.getInstance(Presenters::class.java)

        subscription.cancel()
        message1 = "Done loading."

        // Save the total amount of DI components detected into a file, so next loading screen will be more accurate.
        userConfig.diComponents = componentCount

        injector
    }

    private class LifecycleModule(private val listener: ProvisionListener) : AbstractModule() {
        override fun configure() {
            bindListener(Matchers.any(), listener)
        }
    }
}