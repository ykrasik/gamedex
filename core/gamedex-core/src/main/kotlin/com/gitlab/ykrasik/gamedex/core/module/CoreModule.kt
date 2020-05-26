/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.Genre
import com.gitlab.ykrasik.gamedex.GenreId
import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.app.api.common.ViewCommonOps
import com.gitlab.ykrasik.gamedex.core.*
import com.gitlab.ykrasik.gamedex.core.common.CommonOpsConfig
import com.gitlab.ykrasik.gamedex.core.common.ViewCommonOpsImpl
import com.gitlab.ykrasik.gamedex.core.common.presenter.AboutViewPresenter
import com.gitlab.ykrasik.gamedex.core.common.presenter.ExternalHidePresenter
import com.gitlab.ykrasik.gamedex.core.common.presenter.ShowAboutViewPresenter
import com.gitlab.ykrasik.gamedex.core.file.module.FileModule
import com.gitlab.ykrasik.gamedex.core.filter.module.FilterModule
import com.gitlab.ykrasik.gamedex.core.game.module.GameModule
import com.gitlab.ykrasik.gamedex.core.genre.GenreService
import com.gitlab.ykrasik.gamedex.core.genre.GenreServiceImpl
import com.gitlab.ykrasik.gamedex.core.image.module.ImageModule
import com.gitlab.ykrasik.gamedex.core.library.module.LibraryModule
import com.gitlab.ykrasik.gamedex.core.log.module.LogModule
import com.gitlab.ykrasik.gamedex.core.maintenance.module.MaintenanceModule
import com.gitlab.ykrasik.gamedex.core.persistence.module.PersistenceModule
import com.gitlab.ykrasik.gamedex.core.plugin.ClasspathPluginScanner
import com.gitlab.ykrasik.gamedex.core.plugin.DirectoryPluginScanner
import com.gitlab.ykrasik.gamedex.core.plugin.PluginManager
import com.gitlab.ykrasik.gamedex.core.plugin.PluginManagerImpl
import com.gitlab.ykrasik.gamedex.core.provider.module.ProviderModule
import com.gitlab.ykrasik.gamedex.core.settings.*
import com.gitlab.ykrasik.gamedex.core.settings.presenter.*
import com.gitlab.ykrasik.gamedex.core.storage.IntIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.JsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.core.storage.StringIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.task.TaskServiceImpl
import com.gitlab.ykrasik.gamedex.core.task.presenter.TaskPresenter
import com.gitlab.ykrasik.gamedex.core.view.ViewService
import com.gitlab.ykrasik.gamedex.core.view.ViewServiceImpl
import com.gitlab.ykrasik.gamedex.core.web.presenter.BrowseUrlPresenter
import com.gitlab.ykrasik.gamedex.util.time
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.TypeLiteral
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 27/03/2018
 * Time: 21:55
 */
object CoreModule : InternalCoreModule() {
    val configFile = "/com/gitlab/ykrasik/gamedex/core/core.conf"

    override fun configure() {
        installModules()
        bindPresenters()

        bind(TaskService::class.java).to(TaskServiceImpl::class.java)
        bind(EventBus::class.java).to(EventBusImpl::class.java)

        bind(ViewRegistry::class.java).to(ViewRegistryImpl::class.java)
        bind(ViewCommonOps::class.java).to(ViewCommonOpsImpl::class.java)
        bind(ViewService::class.java).to(ViewServiceImpl::class.java)

        bind(SettingsRepository::class.java).to(SettingsRepositoryImpl::class.java)
        bind(GenreService::class.java).to(GenreServiceImpl::class.java)

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
        install(PersistenceModule)
        install(ProviderModule)
        install(FilterModule)
    }

    private fun bindPresenters() {
        bind(ExternalHidePresenter::class.java)

        bindPresenter(TaskPresenter::class)

        bindPresenter(ShowSettingsPresenter::class)
        bindPresenter(SettingsPresenter::class)
        bindPresenter(GeneralSettingsPresenter::class)
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
    fun pluginManager(injector: Injector): PluginManager {
        val scanners = listOf(DirectoryPluginScanner("plugins")).let {
            // In dev scan the plugins dir & classpath
            // In prod scan only the plugins dir
            if (env == Environment.Dev) it + ClasspathPluginScanner() else it
        }
        return PluginManagerImpl(injector, scanners)
    }

    @Provides
    @Singleton
    fun config(): Config = log.time("Loading configuration...") {
        listOf(ImageModule.configFile, PersistenceModule.configFile, configFile)
            .fold(ConfigFactory.load()) { config, file ->
                val resource = checkNotNull(javaClass.getResource(file)) { "Config file not found: $file" }
                config.withFallback(ConfigFactory.parseURL(resource))
            }
    }

    @Provides
    @Singleton
    fun commonConfig(config: Config) = config.extract<CommonOpsConfig>("gameDex.common")

    @Provides
    @Singleton
    @NameDisplaySettingsRepository
    fun nameDisplaySettingsRepository(repo: SettingsRepository) = GameOverlayDisplaySettingsRepository.name(repo)

    @Provides
    @Singleton
    @MetaTagDisplaySettingsRepository
    fun metaTagDisplaySettingsRepository(repo: SettingsRepository) = GameOverlayDisplaySettingsRepository.metaTag(repo)

    @Provides
    @Singleton
    @VersionDisplaySettingsRepository
    fun versionDisplaySettingsRepository(repo: SettingsRepository) = GameOverlayDisplaySettingsRepository.version(repo)

    @Provides
    @Singleton
    fun genreStorage(): Storage<GenreId, Genre> = StringIdJsonStorageFactory("data/genres")
}