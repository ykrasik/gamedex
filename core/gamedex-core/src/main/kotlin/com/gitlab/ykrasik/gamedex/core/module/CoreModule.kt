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

package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.app.api.game.delete.DeleteGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.details.GameDetailsPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGameChooseResultsPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverGamesWithoutProvidersPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.DiscoverNewGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.discover.RediscoverGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.download.GameDownloadStaleDurationPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.download.RedownloadAllStaleGamesPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.download.RedownloadGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.edit.EditGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.rename.RenameMoveGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGamePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.CleanupDbPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ClearUserDataPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ExportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.general.ImportDatabasePresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.library.*
import com.gitlab.ykrasik.gamedex.app.api.log.LogEntriesPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.log.LogLevelPresenterFactory
import com.gitlab.ykrasik.gamedex.app.api.log.LogTailPresenterFactory
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.image.ImageRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.file.FileSystemServiceImpl
import com.gitlab.ykrasik.gamedex.core.file.NewDirectoryDetector
import com.gitlab.ykrasik.gamedex.core.game.GameConfig
import com.gitlab.ykrasik.gamedex.core.game.GameServiceImpl
import com.gitlab.ykrasik.gamedex.core.game.GameUserConfig
import com.gitlab.ykrasik.gamedex.core.game.common.DeleteGamePresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.details.GameDetailsPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGameChooseResultsPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverGamesWithoutProvidersPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.discover.DiscoverNewGamesPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.discover.RediscoverGamePresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.download.GameDownloadStaleDurationPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.download.RedownloadAllStaleGamesPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.download.RedownloadGamePresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.edit.EditGamePresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.rename.RenameMoveGamePresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.game.tag.TagGamePresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.general.*
import com.gitlab.ykrasik.gamedex.core.image.ImageConfig
import com.gitlab.ykrasik.gamedex.core.image.ImageRepositoryImpl
import com.gitlab.ykrasik.gamedex.core.library.*
import com.gitlab.ykrasik.gamedex.core.log.LogEntriesPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.log.LogLevelPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.log.LogTailPresenterFactoryImpl
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderServiceImpl
import com.gitlab.ykrasik.gamedex.core.provider.ProviderUserConfig
import com.gitlab.ykrasik.gamedex.core.report.ReportUserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfig
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
import com.gitlab.ykrasik.gamedex.core.util.ClassPathScanner
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 27/03/2018
 * Time: 21:55
 */
object CoreModule : AbstractModule() {
    override fun configure() {
        bind(LibraryService::class.java).to(LibraryServiceImpl::class.java)
        bind(GameService::class.java).to(GameServiceImpl::class.java)
        bind(GameProviderService::class.java).to(GameProviderServiceImpl::class.java)

        bind(FileSystemService::class.java).to(FileSystemServiceImpl::class.java)
        bind(ImageRepository::class.java).to(ImageRepositoryImpl::class.java)

        bind(EditGamePresenterFactory::class.java).to(EditGamePresenterFactoryImpl::class.java)
        bind(DeleteGamePresenterFactory::class.java).to(DeleteGamePresenterFactoryImpl::class.java)
        bind(RenameMoveGamePresenterFactory::class.java).to(RenameMoveGamePresenterFactoryImpl::class.java)
        bind(TagGamePresenterFactory::class.java).to(TagGamePresenterFactoryImpl::class.java)

        bind(GameDetailsPresenterFactory::class.java).to(GameDetailsPresenterFactoryImpl::class.java)

        bind(DiscoverGameChooseResultsPresenterFactory::class.java).to(DiscoverGameChooseResultsPresenterFactoryImpl::class.java)
        bind(DiscoverGamesWithoutProvidersPresenterFactory::class.java).to(DiscoverGamesWithoutProvidersPresenterFactoryImpl::class.java)
        bind(DiscoverNewGamesPresenterFactory::class.java).to(DiscoverNewGamesPresenterFactoryImpl::class.java)
        bind(RediscoverGamePresenterFactory::class.java).to(RediscoverGamePresenterFactoryImpl::class.java)

        bind(GameDownloadStaleDurationPresenterFactory::class.java).to(GameDownloadStaleDurationPresenterFactoryImpl::class.java)
        bind(RedownloadAllStaleGamesPresenterFactory::class.java).to(RedownloadAllStaleGamesPresenterFactoryImpl::class.java)
        bind(RedownloadGamePresenterFactory::class.java).to(RedownloadGamePresenterFactoryImpl::class.java)

        bind(AddLibraryPresenterFactory::class.java).to(AddLibraryPresenterFactoryImpl::class.java)
        bind(DeleteLibraryPresenterFactory::class.java).to(DeleteLibraryPresenterFactoryImpl::class.java)
        bind(EditLibraryPresenterFactory::class.java).to(EditLibraryPresenterFactoryImpl::class.java)
        bind(LibrariesPresenterFactory::class.java).to(LibrariesPresenterFactoryImpl::class.java)
        bind(EditLibraryViewPresenterFactory::class.java).to(EditLibraryViewPresenterFactoryImpl::class.java)

        bind(LogEntriesPresenterFactory::class.java).to(LogEntriesPresenterFactoryImpl::class.java)
        bind(LogLevelPresenterFactory::class.java).to(LogLevelPresenterFactoryImpl::class.java)
        bind(LogTailPresenterFactory::class.java).to(LogTailPresenterFactoryImpl::class.java)

        bind(ExportDatabasePresenterFactory::class.java).to(ExportDatabasePresenterFactoryImpl::class.java)
        bind(ImportDatabasePresenterFactory::class.java).to(ImportDatabasePresenterFactoryImpl::class.java)
        bind(ClearUserDataPresenterFactory::class.java).to(ClearUserDataPresenterFactoryImpl::class.java)
        bind(CleanupDbPresenterFactory::class.java).to(CleanupDbPresenterFactoryImpl::class.java)

        with(Multibinder.newSetBinder(binder(), UserConfig::class.java)) {
            addBinding().to(GameUserConfig::class.java)
            addBinding().to(GeneralUserConfig::class.java)
            addBinding().to(ProviderUserConfig::class.java)
            addBinding().to(ReportUserConfig::class.java)
        }
        bind(UserConfigRepository::class.java)
    }

    @Provides
    @Singleton
    fun config(): Config {
        val configurationFiles = ClassPathScanner.scanResources("com.gitlab.ykrasik.gamedex") {
            it.endsWith(".conf") && it != "application.conf" && it != "reference.conf"
        }

        // Use the default config as a baseline and apply 'withFallback' on it for every custom .conf file encountered.
        return configurationFiles.fold(ConfigFactory.load()) { current, url ->
            current.withFallback(ConfigFactory.parseURL(url))
        }
    }

    @Provides
    @Singleton
    fun newDirectoryDetector(config: Config) =
        Class.forName(config.getString("gameDex.newDirectoryDetector.class")).newInstance() as NewDirectoryDetector

    @Provides
    @Singleton
    fun gameConfig(config: Config): GameConfig = config.extract("gameDex.game")

    @Provides
    @Singleton
    fun imageConfig(config: Config): ImageConfig = config.extract("gameDex.image")
}