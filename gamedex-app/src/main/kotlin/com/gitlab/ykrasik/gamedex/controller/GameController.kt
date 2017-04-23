package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.ChangeThumbnailFragment
import javafx.beans.property.SimpleListProperty
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.Controller
import tornadofx.SortedFilteredList

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
class GameController : Controller() {
    private val gameRepository: GameRepository by di()
    private val libraryRepository: LibraryRepository by di()
    private val libraryScanner: LibraryScanner by di()
    private val notificationManager: NotificationManager by di()

    val games: SortedFilteredList<Game> = SortedFilteredList(gameRepository.games)
    val gamesProperty = SimpleListProperty(games)

    fun refreshGames() = launch(CommonPool) {
        val excludedPaths =
            libraryRepository.libraries.map(Library::path).toSet() +
                gameRepository.games.map(Game::path).toSet()

        libraryRepository.libraries.asSequence()
            .filter { it.platform != GamePlatform.excluded }
            .forEach { library ->
                val (job, notification) = libraryScanner.refresh(library, excludedPaths - library.path, context)
                notificationManager.bind(notification)
                for (addGameRequest in job.channel) {
                    val game = gameRepository.add(addGameRequest)
                    notification.message = "[${addGameRequest.metaData.path}] Done: $game"
                }
                notificationManager.unbind()
            }
    }

    fun changeThumbnail(game: Game) = launch(JavaFx) {
        val (thumbnailOverride, newThumbnailUrl) = ChangeThumbnailFragment(game).show() ?: return@launch
        if (newThumbnailUrl != game.thumbnailUrl) {
            val newRawGame = game.rawGame.withPriorityOverride { it.copy(thumbnail = thumbnailOverride) }
            gameRepository.update(newRawGame)
        }
    }

    fun delete(game: Game) = launch(JavaFx) {
        if (areYouSureDialog("Delete game '${game.name}'?")) {
            gameRepository.delete(game)
        }
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
}