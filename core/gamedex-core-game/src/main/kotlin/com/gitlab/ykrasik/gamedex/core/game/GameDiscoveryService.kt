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
import com.gitlab.ykrasik.gamedex.core.CommonData
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.task.Task
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.nowTimestamp
import com.google.inject.ImplementedBy
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/04/2018
 * Time: 13:42
 */
@ImplementedBy(GameDiscoveryServiceImpl::class)
interface GameDiscoveryService {
    fun discoverNewGames(): Task<Unit>
    fun rediscoverGamesWithMissingProviders(): Task<Unit>
    fun rediscoverGame(game: Game): Task<Game?>
}

@Singleton
class GameDiscoveryServiceImpl @Inject constructor(
    private val gameService: GameService,
    private val commonData: CommonData,
    private val fileSystemService: FileSystemService,
    private val gameProviderService: GameProviderService
) : GameDiscoveryService {
    override fun discoverNewGames() = task("Discovering New Games...", isCancellable = true) {
        gameProviderService.checkAtLeastOneProviderEnabled()

        message = "Detecting new directories..."
        val newDirectories = detectNewDirectories()
        message = "Detecting new directories... ${newDirectories.size} new games."

        var added = 0
        successOrCancelledMessage { success ->
            if (newDirectories.isEmpty()) {
                "No new games detected."
            } else {
                "${if (success) "Done" else "Cancelled"}: Added $added / ${newDirectories.size} new games."
            }
        }

        newDirectories.forEachWithProgress { (library, directory) ->
            val addGameRequest = processDirectory(directory, library)
            if (addGameRequest != null) {
                executeSubTask(gameService.add(addGameRequest))
                added += 1
            }
        }
    }

    private fun Task<*>.detectNewDirectories(): List<Pair<Library, File>> {
        val excludedDirectories = commonData.realLibraries.map(Library::path).toSet() + gameService.games.map(Game::path)

        return commonData.realLibraries.flatMapWithProgress { library ->
            fileSystemService.detectNewDirectories(library.path, excludedDirectories - library.path)
                .map { library to it }
        }
    }

    private suspend fun Task<*>.processDirectory(directory: File, library: Library): AddGameRequest? {
        val results = executeSubTask(gameProviderService.search(directory.name, library.platform, directory, excludedProviders = emptyList()))
            ?: return null
        return AddGameRequest(
            metadata = Metadata(
                libraryId = library.id,
                path = directory.relativeTo(library.path).path,
                timestamp = nowTimestamp
            ),
            providerData = results.providerData,
            userData = if (results.excludedProviders.isNotEmpty()) {
                UserData(excludedProviders = results.excludedProviders)
            } else {
                null
            }
        )
    }

    override fun rediscoverGamesWithMissingProviders(): Task<Unit> {
        val gamesWithMissingProviders = gameService.games.filter { it.hasMissingProviders }
        return rediscoverGames(gamesWithMissingProviders)
    }

    private val Game.hasMissingProviders: Boolean
        get() = gameProviderService.enabledProviders.any { provider ->
            // Provider supports the game's platform, is not excluded and game doesn't have it yet.
            provider.supports(platform) && !excludedProviders.contains(provider.id) && rawGame.providerData.none { it.header.id == provider.id }
        }

    private fun rediscoverGames(games: List<Game>) = task("Re-Discovering ${games.size} Games...") {
        gameProviderService.checkAtLeastOneProviderEnabled()

        message = "Re-Discovering ${games.size} Games..."
        var processed = 0
        successOrCancelledMessage { success ->
            "${if (success) "Done" else "Cancelled"}: Re-Discovered $processed / $totalItems Games."
        }

        // Operate on a copy of the games to avoid concurrent modifications
        games.sortedBy { it.name }.forEachWithProgress { game ->
            val excludedProviders = game.existingProviders + game.excludedProviders
            if (rediscoverGame(game, excludedProviders) != null) {
                processed += 1
            }
        }
    }

    override fun rediscoverGame(game: Game) = task("Re-Discovering '${game.name}'...") {
        gameProviderService.checkAtLeastOneProviderEnabled()

        successMessage = { "Done: Re-Discovered '${game.name}'." }
        rediscoverGame(game, excludedProviders = emptyList())
    }

    private suspend fun Task<*>.rediscoverGame(game: Game, excludedProviders: List<ProviderId>): Game? {
        val results = executeSubTask(gameProviderService.search(game.name, game.platform, game.path, excludedProviders))
        if (results?.isEmpty() != false) return null

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

    private suspend fun Task<*>.updateGame(game: Game, newProviderData: List<ProviderData>, newUserData: UserData?): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData, userData = newUserData)
        return executeSubTask(gameService.replace(game, newRawGame))
    }
}