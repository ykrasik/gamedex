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

import com.gitlab.ykrasik.gamedex.app.api.log.LogLevel
import com.gitlab.ykrasik.gamedex.app.api.preloader.Preloader
import com.gitlab.ykrasik.gamedex.app.api.preloader.PreloaderView
import com.gitlab.ykrasik.gamedex.core.api.util.uiThreadDispatcher
import com.gitlab.ykrasik.gamedex.core.log.GamedexLog
import com.gitlab.ykrasik.gamedex.core.log.GamedexLogAppender
import com.gitlab.ykrasik.gamedex.core.module.CoreModule
import com.gitlab.ykrasik.gamedex.core.settings.PreloaderSettingsRepository
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.millisTaken
import com.gitlab.ykrasik.gamedex.util.toHumanReadableDuration
import com.google.inject.*
import com.google.inject.matcher.Matchers
import com.google.inject.spi.ProvisionListener
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.slf4j.bridge.SLF4JBridgeHandler

/**
 * User: ykrasik
 * Date: 13/04/2018
 * Time: 09:06
 */
class PreloaderImpl : Preloader {
    private val log = logger()

    override suspend fun load(view: PreloaderView, vararg extraModules: Module): Injector = withContext(CommonPool) {
        val (injector, millisTaken) = millisTaken {
            SLF4JBridgeHandler.removeHandlersForRootLogger()
            SLF4JBridgeHandler.install()
            GamedexLogAppender.init()

            withContext(uiThreadDispatcher) {
                view.progress = 0.0
                view.message = "Loading..."
            }

            // While loading, display all log messages in the task
            val subscription = GamedexLog.entries.itemsChannel.subscribe(uiThreadDispatcher) {
                it.lastOrNull()?.let {
                    if (it.level.ordinal >= LogLevel.Info.ordinal) {
                        view.message = it.message
                    }
                }
            }

            val preloaderSettings = PreloaderSettingsRepository()

            var componentCount = 0

            val loadProgressModule = object : AbstractModule() {
                override fun configure() {
                    bindListener(Matchers.any(), object : ProvisionListener {
                        override fun <T : Any?> onProvision(provision: ProvisionListener.ProvisionInvocation<T>?) {
                            componentCount++
                            launch(uiThreadDispatcher) {
                                try {
                                    view.progress = componentCount.toDouble() / preloaderSettings.diComponents
                                } catch (_: ClosedSendChannelException) {
                                    // This happens when we don't pre-load all required classes during this stage
                                    // Some classes get lazily loaded, which will trigger this exception.
                                    println("Lazy loading: ${provision!!.binding.key}")  // TODO: Temp
                                    // FIXME: Make sure everything is pre-loaded!
                                }
                            }
                        }
                    })
                }
            }
            val injector = Guice.createInjector(Stage.PRODUCTION, CoreModule, loadProgressModule, *extraModules)

            subscription.cancel()

            // Save the total amount of DI components detected into a file, so next loading screen will be more accurate.
            preloaderSettings.modify { copy(diComponents = componentCount) }

            withContext(uiThreadDispatcher) {
                view.message = "Done loading."
            }

            injector
        }
        log.info("Application loaded in ${millisTaken.toHumanReadableDuration()}")

        return@withContext injector
    }
}