package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.util.now
import kotlinx.coroutines.experimental.channels.produce
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 02/05/2017
 * Time: 13:03
 */
@Singleton
class GameTasks @Inject constructor(
    private val newDirectoryDetector: NewDirectoryDetector,
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val providerRepository: GameProviderRepository,
    private val providerService: GameProviderService     // TODO: This class appears in too many places.
) {
    inner class ScanNewGamesTask(private val searchMode: GameProviderService.SearchConstraints.SearchMode) : Task<Unit>("Scanning new games...") {
        private var numNewGames = 0

        override suspend fun doRun() {
            val requestsJob = scan()
            for (addGameRequest in requestsJob.channel) {
                gameRepository.add(addGameRequest)
                numNewGames += 1
            }
        }

        private fun scan() = produce<AddGameRequest>(context) {
            progress.message = "Scanning for new games..."

            val libraries = libraryRepository.libraries
            val games = gameRepository.games
            val excludedDirectories = libraries.map(Library::path).toSet() + games.map(Game::path).toSet()
            val newDirectories = libraries.flatMap { library ->
                val paths = if (library.platform != Platform.excluded) {
                    log.debug("Scanning library: $library")
                    newDirectoryDetector.detectNewDirectories(library.path, excludedDirectories - library.path).apply {
                        log.debug("Found ${this.size} new games.")
                    }
                } else {
                    emptyList()
                }
                paths.map { library to it }
            }

            progress.message = "Scanning for new games: ${newDirectories.size} new games."

            newDirectories.forEachIndexed { i, (library, directory) ->
                val addGameRequest = processDirectory(directory, library)
                progress.progress(i, newDirectories.size)
                addGameRequest?.let { send(it) }
            }
            channel.close()
        }

        private suspend fun processDirectory(directory: File, library: Library): AddGameRequest? {
            val taskData = GameProviderService.ProviderTaskData(this, directory.name, library.platform, directory)
            val constraints = GameProviderService.SearchConstraints(mode = searchMode, excludedProviders = emptyList())
            val results = providerService.search(taskData, constraints) ?: return null
            val relativePath = library.path.toPath().relativize(directory.toPath()).toFile()
            val metaData = MetaData(library.id, relativePath, updateDate = now)
            val userData = if (results.excludedProviders.isNotEmpty()) {
                UserData(excludedProviders = results.excludedProviders)
            } else {
                null
            }
            return AddGameRequest(
                metaData = metaData,
                providerData = results.providerData,
                userData = userData
            )
        }

        override fun doneMessage() = "Done: Added $numNewGames new games."
    }

    inner class CleanupTask : Task<Unit>("Cleaning up...") {
        private var staleGames = 0
        private var staleLibraries = 0

        override suspend fun doRun() {
            // TODO: First detect stale, then confirm, then delete.
            // TODO: Create backup before deleting
            staleGames = cleanupStaleGames()
            staleLibraries = cleanupStaleLibraries()
        }

        private suspend fun cleanupStaleGames(): Int {
            progress.message = "Detecting stales games..."
            val staleGames = gameRepository.games.filterIndexed { i, game ->
                (!game.path.isDirectory).apply {
                    progress.progress(i, gameRepository.games.size)
                }
            }

            progress.message = "Cleaning up ${staleGames.size} stales games..."
            gameRepository.deleteAll(staleGames, progress)

            return staleGames.size
        }

        private suspend fun cleanupStaleLibraries(): Int {
            progress.message = "Detecting stales libraries..."
            val staleLibraries = libraryRepository.libraries.filterIndexed { i, library ->
                (!library.path.isDirectory).apply {
                    progress.progress(i, libraryRepository.libraries.size)
                }
            }

            progress.message = "Cleaning up ${staleLibraries.size} stales libraries..."
            libraryRepository.deleteAll(staleLibraries, progress)
            return staleLibraries.size
        }

        override fun doneMessage() = "Done cleaning up: Removed $staleGames stale games and $staleLibraries stale libraries."
    }

    // TODO: Consider renaming 'refresh' to 'redownload'
    // TODO: Add the fact that this only operates on stale games to it's name.
    inner class RefreshAllGamesTask : Task<Unit>("Refreshing all games...") {
        private var numRefreshed = 0

        override suspend fun doRun() {
            // Operate on a copy of the games to avoid concurrent modifications
            gameRepository.games.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, gameRepository.games.size - 1)

                val providersToDownload = game.providerHeaders.filter { header ->
                    // TODO: Store stale duration as config or parameter.
                    header.updateDate.plusMonths(1).isBeforeNow
                }
                if (providersToDownload.isNotEmpty()) {
                    doRefreshGame(game, providersToDownload)
                    numRefreshed += 1
                }
            }
        }

        override fun doneMessage() = "Done: Refreshed $numRefreshed games."
    }

    inner class RefreshGameTask(private val game: Game) : Task<Game>("Refreshing '${game.name}'...") {
        override suspend fun doRun() = doRefreshGame(game)
        override fun doneMessage() = "Done refreshing: '${game.name}'."
    }

    private suspend fun Task<*>.doRefreshGame(game: Game, providersToDownload: List<ProviderHeader> = game.providerHeaders): Game {
        val taskData = GameProviderService.ProviderTaskData(this, game.name, game.platform, game.path)
        val downloadedProviderData = providerService.download(taskData, providersToDownload)
        val newProviderData = if (providersToDownload == game.providerHeaders) {
            downloadedProviderData
        } else {
            game.rawGame.providerData.filterNot { d -> providersToDownload.any { it.type == d.header.type } } + downloadedProviderData
        }
        return updateGame(game, newProviderData, newUserData = game.userData)
    }

    // TODO: This only rediscovers non-excluded providers - find a way to add to name
    inner class RediscoverAllGamesTask : Task<Unit>("Rediscovering all games...") {
        private var numRetried = 0
        private var numSucceeded = 0

        override suspend fun doRun() {
            // Operate on a copy of the games to avoid concurrent modifications
            val gamesToRetry = gameRepository.games.filter { it.hasMissingProviders }
            gamesToRetry.sortedBy { it.name }.forEachIndexed { i, game ->
                progress.progress(i, gamesToRetry.size - 1)

                val excludedProviders = game.existingProviders + game.excludedProviders
                if (doSearchAgain(game, excludedProviders = excludedProviders) != null) {
                    numSucceeded += 1
                }
                numRetried += 1
            }
        }

        // TODO: Can consider checking if the missing providers support the game's platform, to avoid an empty call.
        private val Game.hasMissingProviders: Boolean
            get() = rawGame.providerData.size + excludedProviders.size < providerRepository.providers.size

        private val Game.existingProviders get() = rawGame.providerData.map { it.header.type }
        private val Game.excludedProviders get() = userData?.excludedProviders ?: emptyList()

        override fun doneMessage() = "Done: Rediscovered $numSucceeded/$numRetried games."
    }

    inner class SearchGameTask(private val game: Game) : Task<Game>("Searching '${game.name}'...") {
        override suspend fun doRun() = doSearchAgain(game, excludedProviders = emptyList()) ?: game
        override fun doneMessage() = "Done searching: '${game.name}'."
    }

    private suspend fun Task<*>.doSearchAgain(game: Game, excludedProviders: List<GameProviderType>): Game? {
        val taskData = GameProviderService.ProviderTaskData(this, game.name, game.platform, game.path)
        val constraints = GameProviderService.SearchConstraints(
            mode = GameProviderService.SearchConstraints.SearchMode.alwaysAsk,
            excludedProviders = excludedProviders
        )
        val results = providerService.search(taskData, constraints) ?: return null
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

    private suspend fun updateGame(game: Game, newProviderData: List<ProviderData>, newUserData: UserData?): Game {
        val newRawGame = game.rawGame.copy(providerData = newProviderData, userData = newUserData)
        return gameRepository.update(newRawGame)
    }

    // TODO: Finish this.
    inner class DetectDuplicateGames : Task<List<Game>>("Detecting duplicate games...") {
        private var duplicates = 0

        override suspend fun doRun() = gameRepository.games.let { games ->
            games.filterIndexed { i, game ->
                (games.any { it != game && it.name == game.name }).apply {
                    if (this) duplicates += 1
                    progress.progress(i, games.size)
                }
            }
        }

        override fun doneMessage() = "Detected $duplicates duplicate games."
    }
}