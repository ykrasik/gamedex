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

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.api.game.GamePresenter
import com.gitlab.ykrasik.gamedex.core.api.game.GameRepository
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryRepository
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderRepository
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.api.provider.ProviderTaskData
import com.gitlab.ykrasik.gamedex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.core.api.task.TaskRunner
import com.gitlab.ykrasik.gamedex.core.api.task.TaskType
import com.gitlab.ykrasik.gamedex.core.userconfig.UserConfigRepository
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
    private val gameRepository: GameRepository,
    private val userConfigRepository: UserConfigRepository,
    private val libraryRepository: LibraryRepository,
    private val fileSystemService: FileSystemService
) : GamePresenter {
    private val gameUserConfig = userConfigRepository[GameUserConfig::class]

    override suspend fun discoverNewGames() = runTask("Discover New Games") {
        val masterTask = this
        message1 = "Detecting new directories..."
        val newDirectories = detectNewDirectories()
        message2 = "${newDirectories.size} new games."

        var added = 0
        doneMessage { success ->
            if (newDirectories.isEmpty()) {
                "No new games detected."
            } else {
                "${if (success) "Done" else "Cancelled"}: Added $added / ${newDirectories.size} new games."
            }
        }

        runSubTask {
            newDirectories.forEachWithProgress(masterTask) { (library, directory) ->
                val addGameRequest = processDirectory(directory, library)
                if (addGameRequest != null) {
                    gameRepository.add(addGameRequest)
                    added += 1
                }
            }
        }
    }

    private fun Task<*>.detectNewDirectories(): List<Pair<Library, File>> {
        val libraries = libraryRepository.libraries.filter { it.platform != Platform.excluded } // TODO: Use RealLibraries from LibraryRepository.
        val excludedDirectories = libraries.map(Library::path).toSet() + gameRepository.games.map(Game::path).toSet()

        return libraries.flatMapWithProgress { library ->
            fileSystemService.detectNewDirectories(library.path, excludedDirectories - library.path)
                .map { library to it }
        }
    }

    private suspend fun Task<*>.processDirectory(directory: File, library: Library): AddGameRequest? {
        val taskData = ProviderTaskData(this, directory.name, library.platform, directory)
        val results = providerService.search(taskData, excludedProviders = emptyList()) ?: return null
        return AddGameRequest(
            metadata = Metadata(
                libraryId = library.id,
                path = directory.relativeTo(library.path).path,
                updateDate = now
            ),
            providerData = results.providerData,
            userData = if (results.excludedProviders.isNotEmpty()) {
                UserData(excludedProviders = results.excludedProviders)
            } else {
                null
            }
        )
    }

    override suspend fun rediscoverGame(game: Game) = runTask("Re-Discover '${game.name}'") {
        doneMessageOrCancelled("Done: Re-Discovered '${game.name}'.")
        rediscoverGame(this, game, excludedProviders = emptyList()) ?: game
    }

    override suspend fun rediscoverAllGamesWithMissingProviders() {
        val gamesWithMissingProviders = gameRepository.games.filter { it.hasMissingProviders }
        rediscoverGames(gamesWithMissingProviders)
    }

    override suspend fun rediscoverGames(games: List<Game>) = runTask("Re-Discover Games") {
        val masterTask = this
        message1 = "Re-Discovering ${games.size} Games..."
        var processed = 0
        doneMessage { success -> "${if (success) "Done" else "Cancelled"}: Re-Discovered $processed / $totalWork Games." }

        runSubTask {
            // Operate on a copy of the games to avoid concurrent modifications
            games.sortedBy { it.name }.forEachWithProgress(masterTask) { game ->
                val excludedProviders = game.existingProviders + game.excludedProviders
                if (rediscoverGame(this, game, excludedProviders) != null) {
                    processed += 1
                }
            }
        }
    }

    private val Game.hasMissingProviders: Boolean
        get() = providerRepository.enabledProviders.any { provider ->
            // Provider supports the game's platform, is not excluded and game doesn't have it yet.
            provider.supports(platform) && !excludedProviders.contains(provider.id) && rawGame.providerData.none { it.header.id == provider.id }
        }

    private suspend fun rediscoverGame(task: Task<*>, game: Game, excludedProviders: List<ProviderId>): Game? {
        val taskData = ProviderTaskData(task, game.name, game.platform, game.path)
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

    override suspend fun redownloadAllGames() = redownloadGames(gameRepository.games)

    override suspend fun redownloadGame(game: Game) = runTask("Re-Download '${game.name}'", TaskType.Quick) {
        doneMessageOrCancelled("Done: Re-Downloaded '${game.name}'.")
        downloadGame(game, game.providerHeaders)
    }

    override suspend fun redownloadGames(games: List<Game>) = runTask("Re-Download Games") {
        val masterTask = this
        message1 = "Re-Downloading ${games.size} Games..."
        doneMessage { success -> "${if (success) "Done" else "Cancelled"}: Re-Downloaded $processed / $totalWork Games." }

        runSubTask {
            // Operate on a copy of the games to avoid concurrent modifications.
            games.sortedBy { it.name }.forEachWithProgress(masterTask) { game ->
                val providersToDownload = game.providerHeaders.filter { header ->
                    header.updateDate.plus(gameUserConfig.stalePeriod).isBeforeNow
                }
                if (providersToDownload.isNotEmpty()) {
                    downloadGame(game, providersToDownload)
                }
            }
        }
    }

    private suspend fun Task<*>.downloadGame(game: Game, requestedProviders: List<ProviderHeader>): Game {
        val taskData = ProviderTaskData(this, game.name, game.platform, game.path)
        val providersToDownload = requestedProviders.filter { providerRepository.isEnabled(it.id) }
        val downloadedProviderData = providerService.download(taskData, providersToDownload)
        // Replace existing data with new data, pass-through any data that wasn't replaced.
        val newProviderData = game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.id == d.header.id } } + downloadedProviderData
        return updateGame(game, newProviderData, game.userData)
    }

    private fun updateGame(game: Game, newProviderData: List<ProviderData>, newUserData: UserData?): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData, userData = newUserData)
        return gameRepository.replace(game, newRawGame)
    }

    private suspend fun <T> runTask(title: String, type: TaskType = TaskType.Long, run: suspend Task<*>.() -> T): T {
        require(providerRepository.enabledProviders.isNotEmpty()) { "No providers are enabled! Please make sure there's at least 1 enabled provider in the settings menu." }
        return taskRunner.runTask(title, type, run = run)
    }
}