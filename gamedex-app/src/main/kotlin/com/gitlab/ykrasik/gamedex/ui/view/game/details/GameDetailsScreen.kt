package com.gitlab.ykrasik.gamedex.ui.view.game.details

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.theme.*
import com.gitlab.ykrasik.gamedex.ui.view.GamedexScreen
import com.gitlab.ykrasik.gamedex.ui.view.game.menu.ChooseSearchResultsToggleMenu
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.ToolBar
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import org.controlsfx.control.PopOver
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
// TODO: This class is too big
class GameDetailsScreen : GamedexScreen("Details", icon = null) {
    private val gameController: GameController by di()
    private val imageLoader: ImageLoader by di()

    private val browser = YouTubeWebBrowser()

    val gameProperty: ObjectProperty<Game> = SimpleObjectProperty()
    var game by gameProperty

    override val useDefaultNavigationButton = false

    override fun ToolBar.constructToolbar() {
        editButton { setOnAction { editDetails() } }
        verticalSeparator()
        tagButton { setOnAction { tag() } }
        verticalSeparator()

        spacer()

        verticalSeparator()
        searchButton {
            enableWhen { gameController.canRunLongTask }
            dropDownMenu(PopOver.ArrowLocation.RIGHT_TOP, closeOnClick = false) {
                ChooseSearchResultsToggleMenu().install(this)
            }
            setOnAction { reloadGame { gameController.searchGame(game).result } }
        }
        verticalSeparator()
        refreshButton {
            enableWhen { gameController.canRunLongTask }
            setOnAction { reloadGame { gameController.refreshGame(game).result } }
        }
        verticalSeparator()
        deleteButton("Delete") {
            setOnAction {
                if (gameController.delete(game)) {
                    goBackScreen()
                }
            }
        }
        verticalSeparator()
    }

    override val root = hbox {
        setId(Style.gameDetailsViewContent)

        // Left
        stackpane {
            setId(Style.leftGameDetailsView)
            addClass(CommonStyle.card)

            val poster = ImageView()
            val gamePosterProperty = gameProperty.flatMap { game ->
                imageLoader.fetchImage(game?.id ?: -1, game?.posterUrl, persistIfAbsent = true)
            }
            poster.imageProperty().bind(gamePosterProperty)

            contextmenu {
                menuitem("Change", graphic = Theme.Icon.poster(20.0)) { editDetails(GameDataType.poster) }
            }

            imageViewResizingPane(poster) {
                maxWidth = screenBounds.width * maxPosterWidthPercent

                // Clip the posterPane's corners to be round after the posterPane's size is calculated.
                clipRectangle {
                    arcWidth = 20.0
                    arcHeight = 20.0
                    heightProperty().bind(this@imageViewResizingPane.heightProperty())
                    widthProperty().bind(this@imageViewResizingPane.widthProperty())
                }
            }
        }

        region { setId(Style.middleGameDetailsView) }

        // TODO: See if this can be made collapsible - squeezebox?
        // Right
        vbox {
            setId(Style.rightGameDetailsView)
            addClass(CommonStyle.card)
            hgrow = Priority.ALWAYS

            // Top
            stackpane {
                gameProperty.perform { game ->
                    if (game != null) {
                        replaceChildren {
                            children += GameDetailsFragment(game, evenIfEmpty = true).root
                        }
                    }
                }
            }

            separator { paddingTop = 10.0 }
            
            // Bottom
            children += browser.root.apply { vgrow = Priority.ALWAYS }
            gameProperty.perform { game ->
                if (game != null) browser.searchYoutube(game)
            }
        }
    }

    init {
        titleProperty.bind(gameProperty.map { it?.name })
    }

    override fun onUndock() {
        browser.stop()
    }

    private fun editDetails(type: GameDataType = GameDataType.name_) = reloadGame {
        gameController.editDetails(game, initialTab = type)
    }

    private fun tag() = reloadGame { gameController.tag(game) }

    private fun reloadGame(f: () -> Deferred<Game>) {
        val newGame = f()
        launch(JavaFx) {
            game = newGame.await()
        }
    }

    private fun goBackScreen() {
        closeRequestedProperty.value = true
    }

    companion object {
        private val maxPosterWidthPercent = 0.44
    }

    class Style : Stylesheet() {
        companion object {
            val gameDetailsViewContent by cssid()
            val leftGameDetailsView by cssid()
            val middleGameDetailsView by cssid()
            val rightGameDetailsView by cssid()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            gameDetailsViewContent {
                padding = box(2.px)
            }

            leftGameDetailsView {
            }

            middleGameDetailsView {
                padding = box(2.px)
            }

            rightGameDetailsView {
                padding = box(5.px)
            }
        }
    }
}