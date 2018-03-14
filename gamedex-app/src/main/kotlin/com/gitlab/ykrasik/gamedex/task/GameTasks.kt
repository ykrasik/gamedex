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
            for (addGameRequest in requestsJob) {
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

        override fun doneMessage() = "Done: Added $numNewGames new games."
    }
}