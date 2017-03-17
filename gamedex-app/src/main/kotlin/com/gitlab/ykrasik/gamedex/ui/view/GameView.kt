package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.LibraryScanner
import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.model.GameSort
import com.gitlab.ykrasik.gamedex.ui.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import com.gitlab.ykrasik.gamedex.ui.model.LibraryRepository
import com.gitlab.ykrasik.gamedex.ui.nonClosableTab
import com.gitlab.ykrasik.gamedex.ui.readOnlyTextField
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 22:14
 */
class GameView : View("Games") {
    private val gameController: GameController by di()
    private val libraryScanner: LibraryScanner by di()

    private val libraryRepository: LibraryRepository by di()
    private val gameRepository: GameRepository by di()

    private val notificationManager: NotificationManager by di()

    private val gameWallView: GameWallView by inject()
    private val gameListView: GameListView by inject()

    override val root = borderpane {

        top {
            toolbar {
                prefHeight = 40.0

                gridpane {
                    hgap = 2.0
                    row {
                        textfield { promptText = "Search" }
                        button(graphic = imageview("/com/gitlab/ykrasik/gamedex/core/ui/x-small-icon.png"))
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    row {
                        button("Genre Filter") { setOnAction { gameController.filterGenres() } }
                        readOnlyTextField()
                        button(graphic = imageview("/com/gitlab/ykrasik/gamedex/core/ui/x-small-icon.png"))
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    row {
                        button("Library Filter") { setOnAction { gameController.filterLibraries() } }
                        readOnlyTextField()
                        button(graphic = imageview("/com/gitlab/ykrasik/gamedex/core/ui/x-small-icon.png"))
                    }
                }

                verticalSeparator(10.0)

                gridpane {
                    hgap = 2.0
                    setMinSize(10.0, 10.0)
                    row {
                        label("Sort:")
                        combobox<GameSort>()
                    }
                }

                verticalSeparator(10.0)

                spacer()

                checkbox("Don't bother me")

                verticalSeparator(10.0)

                button("Refresh Games") {
                    isDefaultButton = true
                    setOnAction { refreshGames() }
                }
            }
        }

        center {
            tabpane {
                nonClosableTab("Wall") { content = gameWallView.root }
                nonClosableTab("List") { content = gameListView.root }
            }
        }
    }

    private fun refreshGames() = launch(CommonPool) {
        val excludedPaths =
            libraryRepository.libraries.map(Library::path).toSet() +
                gameRepository.games.map(Game::path).toSet()

        libraryRepository.libraries.asSequence()
            .filter { it.platform != GamePlatform.excluded }
            .forEach { library ->
                val (job, notification) = libraryScanner.refresh(library, excludedPaths, context)
                notificationManager.bind(notification)
                for (addGameRequest in job.channel) {
                    val game = gameRepository.add(addGameRequest)
                    notification.message = "[${addGameRequest.metaData.path}] Done: $game"
                }
            }
    }
}
