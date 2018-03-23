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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamdex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamdex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamdex.core.api.game.GamePresenter
import com.gitlab.ykrasik.gamdex.core.api.game.GameService
import com.gitlab.ykrasik.gamdex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamdex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamdex.core.api.provider.ProviderTaskData
import com.gitlab.ykrasik.gamdex.core.api.task.Progress
import com.gitlab.ykrasik.gamdex.core.api.task.TaskRunner
import com.gitlab.ykrasik.gamdex.core.api.task.runTask
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.now
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 05/04/2018
 * Time: 10:50
 */
@Singleton
class GamePresenterImpl @Inject constructor(
    private val taskRunner: TaskRunner,
    private val providerRepository: GameProviderRepository,
    private val providerService: GameProviderService,
    private val gameService: GameService,
    private val gameSettings: GameSettings,
    private val libraryService: LibraryService,
    private val fileSystemService: FileSystemService
) : GamePresenter {

    override suspend fun discoverNewGames() = verifyHasProviders {
        taskRunner.runTask("Discovering new games...") { progress ->
            progress.message("Discovering new games...")
            val newDirectories = discoverNewDirectories(progress.subProgress)
            progress.message("Discovering for new games: Found ${newDirectories.size} new games.")

            progress.totalWork = newDirectories.size
            newDirectories.forEach { (library, directory) ->
                val addGameRequest = processDirectory(progress, directory, library)
                progress.inc {
                    if (addGameRequest != null) {
                        gameService.add(addGameRequest).run()
                    }
                }
                progress.doneMessage = "Done: Added ${progress.processed} / ${progress.totalWork} new games."
            }
        }
    }

    private fun discoverNewDirectories(progress: Progress): List<Pair<Library, File>> {
        val libraries = libraryService.libraries.filter { it.platform != Platform.excluded } // TODO: Use RealLibraries from LibraryRepository.
        val games = gameService.games
        val excludedDirectories = libraries.map(Library::path).toSet() + games.map(Game::path).toSet()

        progress.totalWork = libraries.size
        return libraries.flatMap { library ->
            progress.inc {
                fileSystemService.detectNewDirectories(library.path, excludedDirectories - library.path)
                    .map { library to it }
            }
        }
    }

    private suspend fun processDirectory(progress: Progress, directory: File, library: Library): AddGameRequest? {
        val taskData = ProviderTaskData(progress, directory.name, library.platform, directory)
        val results = providerService.search(taskData, excludedProviders = emptyList()) ?: return null
        val relativePath = directory.relativeTo(library.path).path
        val metadata = Metadata(library.id, relativePath, updateDate = now)
        val userData = if (results.excludedProviders.isNotEmpty()) {
            UserData(excludedProviders = results.excludedProviders)
        } else {
            null
        }
        return AddGameRequest(
            metadata = metadata,
            providerData = results.providerData,
            userData = userData
        )
    }

    override suspend fun rediscoverGame(game: Game) = verifyHasProviders {
        taskRunner.runTask("Re-discovering '${game.name}'...") { progress ->
            rediscoverGame(progress, game, excludedProviders = emptyList())?.apply {
                progress.doneMessage = "Re-discovered '${game.name}'."
            } ?: game
        }
    }

    override suspend fun rediscoverAllGamesWithMissingProviders() {
        val gamesWithMissingProviders = gameService.games.filter { it.hasMissingProviders }
        rediscoverGames(gamesWithMissingProviders)
    }

    override suspend fun rediscoverGames(games: List<Game>) = verifyHasProviders {
        taskRunner.runTask("Re-discovering ${games.size} games...") { progress ->
            progress.totalWork = games.size
            // Operate on a copy of the games to avoid concurrent modifications
            games.sortedBy { it.name }.forEach { game ->
                val excludedProviders = game.existingProviders + game.excludedProviders
                if (rediscoverGame(progress.subProgress, game, excludedProviders) != null) {
                    progress.doneMessage = "Done: Re-discovered ${progress.processed} / ${progress.totalWork} Games."
                }
                progress.inc()
            }
        }
    }

    private val Game.hasMissingProviders: Boolean
        get() = providerRepository.enabledProviders.any { provider ->
            // Provider supports the game's platform, is not excluded and game doesn't have it yet.
            provider.supports(platform) && !excludedProviders.contains(provider.id) && rawGame.providerData.none { it.header.id == provider.id }
        }

    private suspend fun rediscoverGame(progress: Progress, game: Game, excludedProviders: List<ProviderId>): Game? {
        val taskData = ProviderTaskData(progress, game.name, game.platform, game.path)
        val results = providerService.search(taskData, excludedProviders) ?: return null
        if (results.isEmpty()) return null

        val newProviderData = if (excludedProviders.isEmpty()) {
            results.providerData
        } else {
            game.rawGame.providerData + results.providerData
        }

        val newUserData = if (results.excludedProviders.isEmpty()) {
            game.userData
        } else {
            game.userData.merge(UserData(excludedProviders = results.excludedProviders))
        }

        return updateGame(game, newProviderData, newUserData)
    }

    private fun UserData?.merge(userData: UserData?): UserData? {
        if (userData == null) return this
        if (this == null) return userData
        return this.merge(userData)
    }

    override suspend fun redownloadAllGames() = verifyHasProviders {
        redownloadGames(gameService.games)
    }

    override suspend fun redownloadGame(game: Game) = verifyHasProviders {
        taskRunner.runTask("Re-downloading '${game.name}'...") { progress ->
            downloadGame(progress, game, game.providerHeaders).apply {
                progress.doneMessage = "Done: Re-downloaded '${game.name}'."
            }
        }
    }

    override suspend fun redownloadGames(games: List<Game>) = verifyHasProviders {
        taskRunner.runTask("Re-downloading ${games.size} games...") { progress ->
            progress.totalWork = games.size

            // Operate on a copy of the games to avoid concurrent modifications.
            games.sortedBy { it.name }.forEach { game ->
                val providersToDownload = game.providerHeaders.filter { header ->
                    header.updateDate.plus(gameSettings.stalePeriod).isBeforeNow
                }
                if (providersToDownload.isNotEmpty()) {
                    downloadGame(progress.subProgress, game, providersToDownload)
                    // TODO: This does not display the last item update, i.e. 499/500
                    progress.doneMessage = "Done: Re-downloaded ${progress.processed} / ${progress.totalWork} Games."
                }
                progress.inc()
            }
        }
    }

    private suspend fun downloadGame(progress: Progress, game: Game, requestedProviders: List<ProviderHeader>): Game {
        val taskData = ProviderTaskData(progress, game.name, game.platform, game.path)
        val providersToDownload = requestedProviders.filter { providerRepository.isEnabled(it.id) }
        val downloadedProviderData = providerService.download(taskData, providersToDownload)
        // Replace existing data with new data, pass-through any data that wasn't replaced.
        val newProviderData = game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.id == d.header.id } } + downloadedProviderData
        return updateGame(game, newProviderData, game.userData)
    }

    private suspend fun updateGame(game: Game, newProviderData: List<ProviderData>, newUserData: UserData?): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData, userData = newUserData)
        return gameService.replace(game, newRawGame)
    }

    private inline fun <T> verifyHasProviders(f: () -> T): T {
        require(providerRepository.enabledProviders.isNotEmpty()) { "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu." }
        return f()
    }
}