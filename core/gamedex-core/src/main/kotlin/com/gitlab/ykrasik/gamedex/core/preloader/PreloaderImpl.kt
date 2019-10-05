/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.app.api.util.conflatedChannel
import com.gitlab.ykrasik.gamedex.core.log.LogService
import com.gitlab.ykrasik.gamedex.core.log.LogServiceImpl
import com.gitlab.ykrasik.gamedex.core.module.CoreModule
import com.gitlab.ykrasik.gamedex.core.storage.StorageObservableImpl
import com.gitlab.ykrasik.gamedex.core.storage.StringIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.util.ValueObservableImpl
import com.gitlab.ykrasik.gamedex.core.util.modify
import com.gitlab.ykrasik.gamedex.core.version.ApplicationVersion
import com.gitlab.ykrasik.gamedex.util.humanReadable
import com.gitlab.ykrasik.gamedex.util.logger
import com.google.inject.*
import com.google.inject.matcher.Matchers
import com.google.inject.spi.ProvisionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.measureTimedValue

/**
 * User: ykrasik
 * Date: 13/04/2018
 * Time: 09:06
 */
class PreloaderImpl : Preloader {
    private val log = logger()

    override suspend fun load(view: PreloaderView, vararg extraModules: Module): Injector = withContext(Dispatchers.IO) {
        val (injector, timeTaken) = measureTimedValue {
            val logService = LogServiceImpl()

            withContext(Dispatchers.Main) {
                view.version.value = ApplicationVersion
                view.progress.value = 0.0
                view.message.value = "Loading..."
            }

            // While loading, display all log messages in the task
            val subscription = logService.entries.itemsChannel.subscribe(Dispatchers.Main) {
                it.lastOrNull()?.let {
                    if (it.level.canLog(LogLevel.Info)) {
                        view.message.value = it.message
                    }
                }
            }

            val preloaderData = StorageObservableImpl(
                valueObservable = ValueObservableImpl(),
                storage = StringIdJsonStorageFactory(basePath = "data"),
                key = "preloader"
            ) {
                PreloaderData(numClassesToInit = 133)
            }

            val progressChannel = conflatedChannel(0.0)
            launch(Dispatchers.Main) {
                progressChannel.consumeEach {
                    view.progress.value = it
                }
            }

            var numClassesToInit = 0
            val loadProgressModule = object : AbstractModule() {
                override fun configure() {
                    bindListener(Matchers.any(), object : ProvisionListener {
                        override fun <T : Any?> onProvision(provision: ProvisionListener.ProvisionInvocation<T>?) {
                            numClassesToInit++
                            log.trace("[$numClassesToInit] Initializing ${provision!!.binding.key.typeLiteral}...")
                            if (!progressChannel.isClosedForSend) {
                                progressChannel.offer(numClassesToInit.toDouble() / preloaderData.value.numClassesToInit)
                            }
                        }
                    })

                    bind(LogService::class.java).toInstance(logService)
                }
            }
            val injector = Guice.createInjector(Stage.PRODUCTION, CoreModule, loadProgressModule, *extraModules)

            progressChannel.close()
            subscription.cancel()

            // Save the total amount of DI components detected into a file, so next loading screen will be more accurate.
            preloaderData.modify { copy(numClassesToInit = numClassesToInit) }
            preloaderData.close()

            withContext(Dispatchers.Main) {
                view.message.value = "Done loading."
            }

            injector
        }
        log.info("Application load time: ${timeTaken.humanReadable}")

        injector
    }

    private data class PreloaderData(val numClassesToInit: Int)
}