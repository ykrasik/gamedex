package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderPriorityOverride
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.core.DataProviderService
import com.gitlab.ykrasik.gamedex.core.GamedexTask
import com.gitlab.ykrasik.gamedex.core.LibraryScanner
import com.gitlab.ykrasik.gamedex.preferences.GameSort
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.ChangeThumbnailFragment
import com.gitlab.ykrasik.gamedex.ui.mapProperty
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import tornadofx.SortedFilteredList
import tornadofx.seconds
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
@Singleton
class GameController @Inject constructor(
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository,
    private val libraryScanner: LibraryScanner,
    private val dataProviderService: DataProviderService,
    userPreferences: UserPreferences
) : Controller() {

    val games = SortedFilteredList(gameRepository.games)

    private val nameComparator = compareBy(Game::name)
    private val criticScoreComparator = compareBy(Game::criticScore).then(nameComparator)
    private val userScoreComparator = compareBy(Game::userScore).then(nameComparator)
    private val releaseDateComparator = compareBy(Game::releaseDate).then(nameComparator)
    private val dateAddedComparator = compareBy(Game::lastModified)

    init {
        games.sortedItems.comparatorProperty().bind(userPreferences.gameSortProperty.mapProperty { it!!.toComparator() })
    }

    fun refreshGames(): RefreshGamesTask = RefreshGamesTask().apply { start() }

    inner class RefreshGamesTask : GamedexTask("Refreshing games...") {
        private var numNewGames = 0

        override suspend fun doRun(context: CoroutineContext) {
            val requestsJob = libraryScanner.scan(context, libraryRepository.libraries, gameRepository.games, progress)
            for (addGameRequest in requestsJob.channel) {
                gameRepository.add(addGameRequest)
                numNewGames += 1
            }
        }

        override fun finally() {
            progress.message = "Done refreshing games: Added $numNewGames new games."
        }
    }

    fun refetchGames(): RefetchGamesTask? {
        return if (areYouSureDialog("Re-fetch all games? This could take a while...")) {
            RefetchGamesTask().apply { start() }
        } else {
            null
        }
    }

    inner class RefetchGamesTask : GamedexTask("Re-fetching games...") {
        private var numRefetched = 0

        override suspend fun doRun(context: CoroutineContext) {
            games.forEachIndexed { i, game ->
                progress.message = "Re-fetching '${game.name}..."
                progress.progress(i, gameRepository.games.size - 1)

                val newRawGameData = dataProviderService.fetch(game.providerData, game.platform)
                val newRawGame = game.rawGame.copy(rawGameData = newRawGameData)
                gameRepository.update(newRawGame)

                numRefetched += 1
            }
        }

        override fun finally() {
            progress.message = "Done: Re-fetched $numRefetched games."
        }
    }

    fun cleanup(): CleanupTask = CleanupTask().apply { start() }

    inner class CleanupTask : GamedexTask("Cleaning up...") {
        private var staleGames = 0
        private var staleLibraries = 0

        suspend override fun doRun(context: CoroutineContext) {
            cleanupStaleGames()
            cleanupStaleLibraries()
        }

        private suspend fun cleanupStaleGames() {
            progress.message = "Detecting stales games..."
            val staleGamesDetected = gameRepository.games.filterIndexed { i, game ->
                progress.progress(i, gameRepository.games.size - 1)
                !game.path.isDirectory
            }
            progress.message = "Detected ${staleGamesDetected.size} stales games."

            staleGamesDetected.forEachIndexed { i, game ->
                progress.message = "Cleaning up stale game: '${game.name}'..."
                progress.progress(i, gameRepository.games.size - 1)

                gameRepository.delete(game)
                staleGames += 1
            }
        }

        private suspend fun cleanupStaleLibraries() {
            progress.message = "Detecting stales libraries..."
            val staleLibrariesDetected = libraryRepository.libraries.filterIndexed { i, library ->
                progress.progress(i, libraryRepository.libraries.size - 1)
                !library.path.isDirectory
            }
            progress.message = "Detected ${staleLibrariesDetected.size} stales libraries."

            staleLibrariesDetected.forEachIndexed { i, library ->
                progress.message = "Deleting stale library: '${library.name}'..."
                progress.progress(i, libraryRepository.libraries.size - 1)

                libraryRepository.delete(library)
                gameRepository.deleteByLibrary(library)  // TODO: This logic is duplicated from LibraryController.
                staleLibraries += 1
            }
        }

        override fun finally() {
            progress.message = "Done cleaning up: Removed $staleGames stale games and $staleLibraries stale libraries."
        }
    }

    fun changeThumbnail(game: Game) = launch(JavaFx) {
        val (thumbnailOverride, newThumbnailUrl) = ChangeThumbnailFragment(game).show() ?: return@launch
        if (newThumbnailUrl != game.thumbnailUrl) {
            val newRawGame = game.rawGame.withPriorityOverride { it.copy(thumbnail = thumbnailOverride) }
            gameRepository.update(newRawGame)
        }
    }

    fun delete(game: Game): Boolean {
        if (!areYouSureDialog("Delete game '${game.name}'?")) return false

        launch(JavaFx) {
            gameRepository.delete(game)

            Notification()
                .text("Deleted game: '${game.name}")
                .information()
                .automaticallyHideAfter(2.seconds)
                .show()
        }
        return true
    }

    fun filterGenres() {
        TODO()  // TODO: Implement
    }

    fun filterLibraries() {
        TODO()  // TODO: Implement
    }

    private fun RawGame.withPriorityOverride(f: (ProviderPriorityOverride) -> ProviderPriorityOverride): RawGame = copy(
        priorityOverride = f(this.priorityOverride ?: ProviderPriorityOverride())
    )

    private fun GameSort.toComparator(): Comparator<Game>? = when (this) {
        GameSort.nameAsc -> nameComparator
        GameSort.nameDesc -> nameComparator.reversed()
        GameSort.criticScoreAsc -> criticScoreComparator
        GameSort.criticScoreDesc -> criticScoreComparator.reversed()
        GameSort.userScoreAsc -> userScoreComparator
        GameSort.userScoreDesc -> userScoreComparator.reversed()
        GameSort.releaseDateAsc -> releaseDateComparator
        GameSort.releaseDateDesc -> releaseDateComparator.reversed()
        GameSort.dateAddedAsc -> dateAddedComparator
        GameSort.dateAddedDesc -> dateAddedComparator.reversed()
        else -> null
    }
}