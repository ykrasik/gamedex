package com.gitlab.ykrasik.gamedex.task

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.Task
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.flatMapIndexed
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/03/2018
 * Time: 22:27
 */
@Singleton
class DatabaseTasks @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val gameRepository: GameRepository,
    private val persistenceService: PersistenceService  // FIXME: Go through an ImageRepository!
) {
    inner class DetectStaleDataTask : Task<StaleData>("Detecting stale data...") {
        private var staleLibraries = 0
        private var staleGames = 0
        private var staleImages = 0

        override suspend fun doRun(): StaleData {
            val libraries = detectStaleLibraries()
            val games = detectStaleGames()
            val images = detectStaleImages()
            return StaleData(libraries, games, images)
        }

        private fun detectStaleLibraries(): List<Library> = detectStaleData("libraries", { staleLibraries = it }) {
            libraryRepository.libraries.filterIndexed { i, library ->
                progress.progress(i, libraryRepository.libraries.size)
                !library.path.isDirectory
            }
        }

        private fun detectStaleGames(): List<Game> = detectStaleData("games", { staleGames = it }) {
            gameRepository.games.filterIndexed { i, game ->
                progress.progress(i, gameRepository.games.size)
                !game.path.isDirectory
            }
        }

        private fun detectStaleImages(): List<Pair<String, FileSize>> = detectStaleData("images", { staleImages = it }) {
            val usedImages = gameRepository.games.flatMapIndexed { i, game ->
                progress.progress(i, gameRepository.games.size)
                game.imageUrls.screenshotUrls + listOf(game.imageUrls.thumbnailUrl, game.imageUrls.posterUrl).mapNotNull { it }
            }
            persistenceService.fetchImagesExcept(usedImages)
        }

        private fun <T> detectStaleData(name: String, setter: (Int) -> Unit, f: () -> List<T>): List<T> {
            progress.message = "Detecting stale $name..."
            val staleData = f()
            setter(staleData.size)
            progress.message = "Detected ${staleData.size} stale $name."
            return staleData
        }

        override fun doneMessage() = if (staleGames == 0 && staleLibraries == 0 && staleImages == 0) {
            "No stale data detected."
        } else {
            listOf(staleGames to "Stale Games", staleLibraries to "Stale Libraries", staleImages to "Stale Images")
                .filter { it.first != 0 }.joinToString("\n") { "${it.first} ${it.second}" }
        }
    }

    inner class CleanupStaleDataTask(private val staleData: StaleData) : Task<Unit>("Cleaning up stale data...") {
        override suspend fun doRun() {
            progress.message = "Removing stale games..."
            progress.progress(0, 3)
            gameRepository.deleteAll(staleData.games)

            progress.message = "Removing stale libraries..."
            progress.progress(1, 3)
            libraryRepository.deleteAll(staleData.libraries)

            progress.message = "Removing stale images..."
            progress.progress(2, 3)
            persistenceService.deleteImages(staleData.images.map { it.first })

            progress.progress(3, 3)
        }

        override fun doneMessage() = "Removed " +
            listOf(staleData.games to "Stale Games", staleData.libraries to "Stale Libraries", staleData.images to "Stale Images (${staleData.staleImagesSize})")
                .filter { it.first.isNotEmpty() }.joinToString("\n") { "${it.first.size} ${it.second}" }
    }

    data class StaleData(
        val libraries: List<Library>,
        val games: List<Game>,
        val images: List<Pair<String, FileSize>>
    ) {
        val isEmpty = libraries.isEmpty() && games.isEmpty() && images.isEmpty()
        val staleImagesSize = images.fold(FileSize(0)) { acc, next -> acc + next.second }
    }

    inner class ClearGameUserDataTask : Task<Unit>("Clearing game user data...") {
        override suspend fun doRun() {
            progress.message = "Clearing game user data..."
            gameRepository.clearUserData()
        }

        override fun doneMessage() = "Cleared all game user data."
    }
}