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

package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.app.api.common.ViewCommonOps
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.EventBusImpl
import com.gitlab.ykrasik.gamedex.core.ViewRegistryImpl
import com.gitlab.ykrasik.gamedex.core.common.AboutViewPresenter
import com.gitlab.ykrasik.gamedex.core.common.CommonOpsConfig
import com.gitlab.ykrasik.gamedex.core.common.ShowAboutViewPresenter
import com.gitlab.ykrasik.gamedex.core.common.ViewCommonOpsImpl
import com.gitlab.ykrasik.gamedex.core.file.module.FileModule
import com.gitlab.ykrasik.gamedex.core.filter.presenter.FilterPresenter
import com.gitlab.ykrasik.gamedex.core.game.module.GameModule
import com.gitlab.ykrasik.gamedex.core.image.module.ImageModule
import com.gitlab.ykrasik.gamedex.core.library.module.LibraryModule
import com.gitlab.ykrasik.gamedex.core.log.module.LogModule
import com.gitlab.ykrasik.gamedex.core.maintenance.module.MaintenanceModule
import com.gitlab.ykrasik.gamedex.core.provider.module.ProviderCoreModule
import com.gitlab.ykrasik.gamedex.core.report.module.ReportModule
import com.gitlab.ykrasik.gamedex.core.settings.presenter.*
import com.gitlab.ykrasik.gamedex.core.storage.IntIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.JsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.StringIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.task.TaskServiceImpl
import com.gitlab.ykrasik.gamedex.core.task.presenter.TaskPresenter
import com.gitlab.ykrasik.gamedex.core.task.presenter.ViewWithRunningTaskPresenter
import com.gitlab.ykrasik.gamedex.core.util.ClassPathScanner
import com.gitlab.ykrasik.gamedex.core.web.presenter.BrowseUrlPresenter
import com.gitlab.ykrasik.gamedex.provider.ProviderModule
import com.gitlab.ykrasik.gamedex.util.time
import com.google.inject.Provides
import com.google.inject.TypeLiteral
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.UserAgent
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 27/03/2018
 * Time: 21:55
 */
object CoreModule : InternalCoreModule() {
    override fun configure() {
        installModules()
        bindPresenters()

        bind(TaskService::class.java).to(TaskServiceImpl::class.java)
        bind(EventBus::class.java).to(EventBusImpl::class.java)

        bind(ViewRegistry::class.java).to(ViewRegistryImpl::class.java)
        bind(ViewCommonOps::class.java).to(ViewCommonOpsImpl::class.java)

        bind(object : TypeLiteral<JsonStorageFactory<Int>>() {}).toInstance(IntIdJsonStorageFactory)
        bind(object : TypeLiteral<JsonStorageFactory<String>>() {}).toInstance(StringIdJsonStorageFactory)
    }

    private fun installModules() {
        install(FileModule)
        install(GameModule)
        install(MaintenanceModule)
        install(ImageModule)
        install(LibraryModule)
        install(LogModule)
        install(ProviderCoreModule)
        install(ReportModule)

        // Install all providers detected by classpath scan
        log.time("Detecting providers...", { time, providers -> "${providers.size} providers in $time" }) {
            val providers = ClassPathScanner.scanSubTypes("com.gitlab.ykrasik.gamedex.provider", ProviderModule::class)
            providers.forEach { install(it.kotlin.objectInstance!!) }
            providers
        }
    }

    private fun bindPresenters() {
        bindPresenter(TaskPresenter::class)
        bindPresenter(ViewWithRunningTaskPresenter::class)

        bindPresenter(FilterPresenter::class)

        bindPresenter(ShowSettingsPresenter::class)
        bindPresenter(SettingsPresenter::class)
        bindPresenter(ChangeGameWallDisplaySettingsPresenter::class)
        bindPresenter(GameWallDisplaySettingsPresenter::class)
        bindPresenter(ChangeGameNameOverlayDisplaySettingsPresenter::class)
        bindPresenter(GameNameOverlayDisplaySettingsPresenter::class)
        bindPresenter(ChangeGameMetaTagOverlayDisplaySettingsPresenter::class)
        bindPresenter(GameMetaTagOverlayDisplaySettingsPresenter::class)
        bindPresenter(ChangeGameVersionOverlayDisplaySettingsPresenter::class)
        bindPresenter(GameVersionOverlayDisplaySettingsPresenter::class)
        bindPresenter(ProviderOrderSettingsPresenter::class)
        bindPresenter(ProviderSettingsPresenter::class)

        bindPresenter(BrowseUrlPresenter::class)

        bindPresenter(ShowAboutViewPresenter::class)
        bindPresenter(AboutViewPresenter::class)
    }

    @Provides
    @Singleton
    fun config(): Config = log.time("Loading configuration...") {
        val configurationFiles = ClassPathScanner.scanResources("com.gitlab.ykrasik.gamedex") {
            it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
        }

        // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
        configurationFiles.fold(ConfigFactory.load()) { current, url ->
            current.withFallback(ConfigFactory.parseURL(url))
        }
    }

    @Provides
    @Singleton
    fun httpClient(): HttpClient = HttpClient(Apache) {
        install(UserAgent)
//        install(Logging) {
//            logger = object : Logger {
//                private val log = com.gitlab.ykrasik.gamedex.util.logger("HttpClient")
//                override fun log(message: String) = log.trace(message)
//            }
//            level = LogLevel.BODY
//        }
    }

    @Provides
    @Singleton
    fun commonConfig(config: Config) = config.extract<CommonOpsConfig>("gameDex.common")
}