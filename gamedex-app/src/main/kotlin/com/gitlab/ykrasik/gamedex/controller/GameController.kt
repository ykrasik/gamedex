package com.gitlab.ykrasik.gamedex.controller

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ProviderPriorityOverride
import com.gitlab.ykrasik.gamedex.RawGame
import com.gitlab.ykrasik.gamedex.core.DataProviderService
import com.gitlab.ykrasik.gamedex.core.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.Notification
import com.gitlab.ykrasik.gamedex.preferences.GameSort
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.fragment.ChangeThumbnailFragment
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.toImageView
import com.gitlab.ykrasik.gamedex.ui.widgets.Notifications
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.SimpleBooleanProperty
import javafx.geometry.Pos
import javafx.scene.layout.GridPane
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ProducerJob
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import tornadofx.*
import javax.inject.Inject
import javax.inject.Singleton

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
        games.sortedItems.comparatorProperty().bind(userPreferences.gameSortProperty.map { it!!.toComparator() })
    }

    fun refreshGames(): RefreshGamesTask = RefreshGamesTask().apply { start() }

    inner class RefreshGamesTask {
        private val log by logger()

        private lateinit var job: ProducerJob<AddGameRequest>
        private val jobNotification = Notification()

        private val notification = Notifications()
            .hideCloseButton()
            .position(Pos.TOP_RIGHT)
            .title("Refreshing games...")
            .graphic(GridPane().apply {
                hgap = 10.0
                vgap = 5.0
                row {
                    progressbar(jobNotification.progressProperty) {
                        minWidth = 500.0
                    }
                    button(graphic = UIResources.Images.error.toImageView()) {
                        setOnAction { job.cancel() }
                    }
                }
                row {
                    text(jobNotification.messageProperty)
                }
            })

        val runningProperty = SimpleBooleanProperty(false)

        fun start() = launch(CommonPool) {
            run(JavaFx) {
                runningProperty.set(true)
                notification.show()
            }

            var newGames = 0
            try {
                run(CommonPool) {
                    job = libraryScanner.scan(context, libraryRepository.libraries, gameRepository.games, jobNotification)
                    for (addGameRequest in job.channel) {
                        val game = gameRepository.add(addGameRequest)
                        jobNotification.message = "Added: '${game.name}"
                        newGames += 1
                    }
                    newGames
                }
            } finally {
                run(JavaFx) {
                    log.info("Done refreshing games: Added $newGames new games.")
                    notification.hide()
                    runningProperty.set(false)
                }
            }
        }
    }

    // TODO: Allow cancelling this
    fun refetchGames() = launch(JavaFx) {
        if (!areYouSureDialog("Re-fetch all games? This could take a while...")) return@launch

        run(CommonPool) {
            games.forEach { game ->
                val newRawGameData = dataProviderService.fetch(game.providerData, game.platform)
                val newRawGame = game.rawGame.copy(rawGameData = newRawGameData)
                gameRepository.update(newRawGame)
            }
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
        if (!areYouSureDialog("Delete game '${game.name}'?")) return@launch

        gameRepository.delete(game)

        Notifications()
            .text("Deleted game: '${game.name}")
            .information()
            .hideAfter(5.seconds)
            .show()
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