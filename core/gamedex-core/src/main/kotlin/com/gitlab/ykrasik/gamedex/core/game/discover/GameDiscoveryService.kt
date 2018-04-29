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

package com.gitlab.ykrasik.gamedex.core.game.discover

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.util.Task
import com.gitlab.ykrasik.gamedex.app.api.util.task
import com.gitlab.ykrasik.gamedex.core.api.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.api.game.AddGameRequest
import com.gitlab.ykrasik.gamedex.core.api.game.GameService
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.api.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.now
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
    private val libraryService: LibraryService,
    private val fileSystemService: FileSystemService,
    private val gameProviderService: GameProviderService
) : GameDiscoveryService {
    override fun discoverNewGames() = task("Discovering New Games...") {
        gameProviderService.checkAtLeastOneProviderEnabled()

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

        // TODO: This reports scan progress to the masterTask and discovery in subTask. Find a better way of doing this.
        runSubTask {
            newDirectories.forEachWithProgress(masterTask) { (library, directory) ->
                val addGameRequest = processDirectory(directory, library)
                if (addGameRequest != null) {
                    runMainTask(gameService.add(addGameRequest))
                    added += 1
                }
            }
        }
    }

    private fun Task<*>.detectNewDirectories(): List<Pair<Library, File>> {
        val excludedDirectories = libraryService.realLibraries.map(Library::path).toSet() + gameService.games.map(Game::path)

        return libraryService.realLibraries.flatMapWithProgress { library ->
            fileSystemService.detectNewDirectories(library.path, excludedDirectories - library.path)
                .map { library to it }
        }
    }

    private suspend fun Task<*>.processDirectory(directory: File, library: Library): AddGameRequest? {
        val results = runMainTask(gameProviderService.search(directory.name, library.platform, directory, excludedProviders = emptyList()))
            ?: return null
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

        val masterTask = this
        message1 = "Re-Discovering ${games.size} Games..."
        var processed = 0
        doneMessage { success -> "${if (success) "Done" else "Cancelled"}: Re-Discovered $processed / $totalWork Games." }

        // TODO: This reports scan progress to the masterTask and discovery in subTask. Find a better way of doing this.
        runSubTask {
            // Operate on a copy of the games to avoid concurrent modifications
            games.sortedBy { it.name }.forEachWithProgress(masterTask) { game ->
                val excludedProviders = game.existingProviders + game.excludedProviders
                if (rediscoverGame(game, excludedProviders) != null) {
                    processed += 1
                }
            }
        }
    }

    override fun rediscoverGame(game: Game) = task("Re-Discovering '${game.name}'...") {
        gameProviderService.checkAtLeastOneProviderEnabled()

        doneMessageOrCancelled("Done: Re-Discovered '${game.name}'.")
        rediscoverGame(game, excludedProviders = emptyList())
    }

    private suspend fun Task<*>.rediscoverGame(game: Game, excludedProviders: List<ProviderId>): Game? {
        val results = runMainTask(gameProviderService.search(game.name, game.platform, game.path, excludedProviders))
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
        return runMainTask(gameService.replace(game, newRawGame))
    }
}