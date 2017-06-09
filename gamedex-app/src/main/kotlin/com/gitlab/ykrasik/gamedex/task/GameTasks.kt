package com.gitlab.ykrasik.gamedex.task

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GameProviderService
import com.gitlab.ykrasik.gamedex.core.NewDirectoryDetector
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
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
// TODO: Tasks should contain minimal logic and act as glue between the logic & display
@Singleton
class GameTasks @Inject constructor(
    private val newDirectoryDetector: NewDirectoryDetector,
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val providerService: GameProviderService
) {
    inner class ScanNewGamesTask : Task<Unit>("Scanning new games...") {
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
            val results = providerService.search(taskData, excludedProviders = emptyList()) ?: return null
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

    inner class DetectStaleDataTask : Task<StaleData>("Detecting stale data...") {
        private var staleLibraries = 0
        private var staleGames = 0

        override suspend fun doRun(): StaleData {
            val libraries = detectStaleLibraries()
            staleLibraries = libraries.size
            val games = detectStaleGames()
            staleGames = games.size
            return StaleData(libraries, games)
        }

        private fun detectStaleLibraries(): List<Library> {
            progress.message = "Detecting stales libraries..."
            val staleLibraries = libraryRepository.libraries.filterIndexed { i, library ->
                progress.progress(i, libraryRepository.libraries.size)
                !library.path.isDirectory
            }
            progress.message = "Detected ${staleLibraries.size} stale libraries."
            return staleLibraries
        }

        private fun detectStaleGames(): List<Game> {
            progress.message = "Detecting stales games..."
            val staleGames = gameRepository.games.filterIndexed { i, game ->
                progress.progress(i, gameRepository.games.size)
                !game.path.isDirectory
            }
            progress.message = "Detected ${staleGames.size} stale games."
            return staleGames
        }

        override fun doneMessage() = if (staleGames == 0 && staleLibraries == 0 ) {
            "No stale data detected."
        } else {
            "Detected $staleGames stale games and $staleLibraries stale libraries."
        }
    }

    inner class CleanupStaleDataTask(private val staleData: StaleData) : Task<Unit>(
        "Cleaning up ${staleData.libraries} stale libraries and ${staleData.games} stale games..."
    ) {
        override suspend fun doRun() {
            libraryRepository.deleteAll(staleData.libraries, progress)
            gameRepository.deleteAll(staleData.games, progress)
        }

        override fun doneMessage() = "Removed ${staleData.libraries} stale libraries and ${staleData.games} stale games."
    }

    data class StaleData(
        val libraries: List<Library>,
        val games: List<Game>
    )

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