/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameDataType
import com.gitlab.ykrasik.gamedex.app.api.file.ViewCanOpenFile
import com.gitlab.ykrasik.gamedex.app.api.game.*
import com.gitlab.ykrasik.gamedex.app.api.image.ViewCanShowImageGallery
import com.gitlab.ykrasik.gamedex.app.api.image.ViewImageParams
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanSyncGame
import com.gitlab.ykrasik.gamedex.app.api.provider.ViewCanUpdateGame
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.app.api.web.ViewCanBrowseUrl
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.common.isNoImage
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import com.gitlab.ykrasik.gamedex.util.*
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.*
import javafx.scene.paint.Color
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class JavaFxGameDetailsView(
    private val canClose: Boolean,
    private val imageFitWidth: Double? = null,
    private val imageFitHeight: Double? = null,
    private val maxDetailsWidth: Double? = null
) : PresentableView(),
    GameDetailsView,
    ViewCanShowImageGallery,
    ViewCanEditGame,
    ViewCanDeleteGame,
    ViewCanRenameMoveGame,
    ViewCanTagGame,
    ViewCanUpdateGame,
    ViewCanSyncGame,
    ViewCanOpenFile,
    ViewCanBrowseUrl {

    private val commonOps: JavaFxCommonOps by di()

    override val gameParams = userMutableState(ViewGameParams(Game.Null, emptyList()))
    override val gameChannel = gameParams.changes.map { it.game }
    private val game = gameParams.property.binding { it.game }

    override val currentGameIndex = state(-1)

    override val canViewNextGame = state(IsValid.valid)
    override val viewNextGameActions = channel<Unit>()

    override val canViewPrevGame = state(IsValid.valid)
    override val viewPrevGameActions = channel<Unit>()

    override val hideViewActions = channel<Unit>()

    override val viewImageActions = channel<ViewImageParams>()
    override val editGameActions = channel<EditGameParams>()
    override val deleteGameActions = channel<Game>()
    override val renameMoveGameActions = channel<RenameMoveGameParams>()
    override val tagGameActions = channel<Game>()

    override val canUpdateGame = state(IsValid.valid)
    override val updateGameActions = channel<Game>()

    override val canSyncGame = state(IsValid.valid)
    override val syncGameActions = channel<Game>()

    override val openFileActions = channel<File>()
    override val browseUrlActions = channel<String>()

    private val noBackground = Background(BackgroundFill(Colors.cloudyKnoxville, CornerRadii.EMPTY, Insets.EMPTY))
    private val noBackgroundProperty = noBackground.toProperty()

    private var slideDirection = ViewTransition.Direction.LEFT

    val customizeOverlay: OverlayPane.OverlayLayer.() -> Unit = {
        val overlayPane = this

        stackpane {
            addClass(Style.arrowLeftContainer)
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
            stackpaneConstraints { alignment = Pos.CENTER_LEFT }
            visibleProperty().bind(overlayPane.hidingProperty.not() and overlayPane.activeProperty)
            jfxButton(graphic = Icons.arrowLeftBold.size(120)) {
                addClass(Style.arrowLeft)
                visibleWhen(canViewPrevGame)
                scaleOnMouseOver(duration = 0.2.seconds, target = 1.1)
                overlayPane.addEventFilter(KEY_PRESSED) { e ->
                    if (e.code == KeyCode.LEFT && canViewPrevGame.value.isSuccess) {
                        flashColor(duration = 0.2.seconds, from = Colors.transparentWhite, to = Color.WHITE)
                        flashScale(duration = 0.2.seconds, target = 1.1)
                        fire()
                    }
                }
                action(viewPrevGameActions) { slideDirection = ViewTransition.Direction.RIGHT }
            }
        }

        stackpane {
            addClass(Style.arrowRightContainer)
            maxWidth = Region.USE_PREF_SIZE
            maxHeight = Region.USE_PREF_SIZE
            stackpaneConstraints { alignment = Pos.CENTER_RIGHT }
            visibleProperty().bind(overlayPane.hidingProperty.not() and overlayPane.activeProperty)
            jfxButton(graphic = Icons.arrowRightBold.size(120)) {
                addClass(Style.arrowRight)
                visibleWhen(canViewNextGame)
                scaleOnMouseOver(duration = 0.2.seconds, target = 1.1)
                overlayPane.addEventFilter(KEY_PRESSED) { e ->
                    if (e.code == KeyCode.RIGHT && canViewNextGame.value.isSuccess) {
                        flashColor(duration = 0.2.seconds, from = Colors.transparentWhite, to = Color.WHITE)
                        flashScale(duration = 0.2.seconds, target = 1.1)
                        fire()
                    }
                }
                action(viewNextGameActions) { slideDirection = ViewTransition.Direction.LEFT }
            }
        }
    }

    override val root = borderpane {
        addClass(Style.gameDetailsView)
        top = createToolbar()
        center = createBody()

        addEventFilter(KEY_PRESSED) { e ->
            if (e.code == KeyCode.S && e.isControlDown) {
                val screenshotUrls = game.value.screenshotUrls
                if (screenshotUrls.isNotEmpty()) {
                    viewImageActions.offer(
                        ViewImageParams(imageUrl = screenshotUrls.first(), imageUrls = screenshotUrls)
                    )
                }
            }
        }
    }

    private fun createToolbar() = prettyToolbar {
        if (canClose) {
            cancelButton("Close") { action(hideViewActions) }
        }
        spacer()
        editButton("Edit") { action(editGameActions) { EditGameParams(game.value, initialView = GameDataType.Name) } }
        gap()
        toolbarButton("Tag", Icons.tag) { action(tagGameActions) { game.value } }
        gap()
        extraMenu {
            infoButton("Update", graphic = Icons.download) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canUpdateGame)
                action(updateGameActions) { game.value }
            }
            infoButton("Sync", graphic = Icons.sync) {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                enableWhen(canSyncGame)
                action(syncGameActions) { game.value }
            }

            verticalGap()

            warningButton("Rename/Move Folder", Icons.folderEdit) {
                action(renameMoveGameActions) { RenameMoveGameParams(game.value, initialSuggestion = null) }
            }
            deleteButton("Delete") {
                useMaxWidth = true
                alignment = Pos.CENTER_LEFT
                action(deleteGameActions) { game.value }
            }
        }
    }

    private fun createBody() = stackpane {
        stackpane {
            addClass(Style.gameDetailsBackground)
            // Background screenshot
            backgroundProperty().bind(game.flatMap { game ->
                if (game.screenshotUrls.isNotEmpty()) {
                    val image = commonOps.fetchImage(game.screenshotUrls.first(), persist = true)
                    image.binding {
                        if (it.isNoImage) {
                            noBackground
                        } else {
                            Background(
                                BackgroundImage(
                                    it,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.CENTER,
                                    BackgroundSize(1.0, 1.0, true, true, true, true)
                                )
                            )
                        }
                    }
                } else {
                    noBackgroundProperty
                }
            })
        }

        // Actual content
        stackpane {
            alignment = Pos.TOP_CENTER
            addClass(Style.detailsViewContent)

            var clearingExcessChildren = false
            children.onChange {
                if (!clearingExcessChildren && children.size > 1) {
                    // This can happen when we slide very quickly, before the animation has time to finish playing.
                    // The result is that a previous animation finished playing after we've already started a new animation,
                    // and because ViewTransition cannot be cancelled, the result is more than 1 child appearing on screen.
                    clearingExcessChildren = true
                    val last = children.last()
                    children.clear()
                    children += last
                    clearingExcessChildren = false
                }
            }

            game.typeSafeOnChange { game ->
                val new = gameDisplay(game)
                val current = children.firstOrNull()
                if (current == null) {
                    children += new
                } else {
                    current.replaceWith(new, ViewTransition.FadeThrough(0.2.seconds))
//                    current.fade(0.2.seconds, opacity = 0)
//                    new.fade(0.2.seconds, opacity = 0, reversed = true)
//                    current.replaceWith(new, ViewTransition.Slide(0.2.seconds, slideDirection))
                }
            }
        }
    }

    private fun gameDisplay(game: Game) = gridpane {
        hgap = 10.0
        vgap = 10.0
        maxWidth = Region.USE_PREF_SIZE

        // Top Left
        makeRoundCorners(imageview(commonOps.fetchPoster(game)) {
            fitWidth = imageFitWidth ?: 0.0
            fitHeight = imageFitHeight ?: 0.0
        }) {
            gridpaneConstraints { columnRowIndex(0, 0); rowSpan = 2; vAlignment = VPos.TOP; fillHeight = false }
        }

//        stackpane {
//            alignment = Pos.TOP_RIGHT
//            gridpaneConstraints { columnRowIndex(0, 0); rowSpan = 2; vAlignment = VPos.TOP; hGrow = Priority.ALWAYS; /*fillHeight = false*/ }
//            imageview(commonOps.fetchPoster(game)) {
//                fitWidthProperty().bind(this@stackpane.widthProperty())
//                fitHeightProperty().bind(this@stackpane.heightProperty())
//            }
//            clipRectangle(arc = 20)
//            useMaxSize = true
//            maxHeight = screenBounds.height * 2 / 3
////            imageFitWidth?.let { maxWidth = it }
////            imageFitHeight?.let { maxHeight = it }
//        }

//        stackpane {
//            alignment = Pos.TOP_RIGHT
//            vbox {
//                gridpaneConstraints { columnRowIndex(0, 0); rowSpan = 2; vAlignment = VPos.TOP; hGrow = Priority.ALWAYS; fillWidth = true /*fillHeight = false*/ }
//                val image = commonOps.fetchPoster(game)
//                backgroundProperty().bind(image.binding {
//                    Background(
//                        BackgroundImage(
//                            it,
//                            BackgroundRepeat.NO_REPEAT,
//                            BackgroundRepeat.NO_REPEAT,
//                            BackgroundPosition(Side.RIGHT, 0.0, true, Side.TOP, 0.0, true),
//                            BackgroundSize(1.0, 1.0, true, true, true, false)
//                        )
//                    )
//                })
//                clipRectangle(arc = 20)
//            }
//        }

        // Top Right
        defaultVbox {
            gridpaneConstraints { columnRowIndex(1, 0); vGrow = Priority.ALWAYS }
            addClass(Style.gameDetails)
            alignment = Pos.TOP_CENTER

            defaultHbox {
                gridpane {
                    hgap = 5.0
                    vgap = 7.0
                    useMaxHeight = true
                    maxDetailsWidth?.let { maxWidth = it }

                    row {
                        icon(game.platform.logo, size = 22)
                        header(game.name) {
                            addClass(Style.name)
                        }
                    }
                    game.releaseDate?.let { releaseDate ->
                        row {
                            icon(Icons.date)
                            label(releaseDate) {
                                addClass(Style.releaseDate)
                                tooltip("Release Date")
                                usePrefWidth = true
                            }
                        }
                    }
                    game.description?.let { description ->
                        row {
                            region()
                            label(description) {
                                addClass(Style.descriptionText)
                                gridpaneConstraints { vAlignment = VPos.TOP }
                            }
                        }
                    }
                    if (game.genres.isNotEmpty()) {
                        row {
                            icon(Icons.masks)
                            flowpane {
                                hgap = 5.0
                                vgap = 3.0
                                tooltip("Genres")
                                game.genres.forEach {
                                    label(it) {
                                        addClass(Style.genreItem)
                                    }
                                }
                            }
                        }
                    }
                    if (game.tags.isNotEmpty()) {
                        row {
                            icon(Icons.tag)
                            flowpane {
                                hgap = 5.0
                                vgap = 3.0
                                tooltip("Tags")
                                game.tags.forEach {
                                    label(it) {
                                        addClass(Style.tag)
                                    }
                                }
                            }
                        }
                    }
                    if (game.filterTags.isNotEmpty()) {
                        row {
                            icon(Icons.tag)
                            flowpane {
                                hgap = 5.0
                                vgap = 3.0
                                tooltip("Tags generated by filters")
                                game.filterTags.forEach {
                                    label(it) {
                                        addClass(Style.tag)
                                    }
                                }
                            }
                        }
                    }
                }
                defaultVbox {
                    alignment = Pos.TOP_RIGHT
                    criticScoreDisplay(game.criticScore)
                    userScoreDisplay(game.userScore)
                }
            }
            spacer()
            stackpane {
                createDate(game.createDate)
                updateDate(game.updateDate)
            }
        }

        // Center Right
        val fileTree = game.fileTree.value
        if (fileTree != null) {
            val absoluteFileTree = fileTree.copy(name = game.path.absolutePath)
            prettyFileTreeView(absoluteFileTree) {
                addClass(Style.fileTree)
                usePrefWidth = true
                prefHeight += 20

                onUserSelect {
                    val file = absoluteFileTree.pathTo(it)!!
                    openFileActions.offer(file)
                }
            }
        } else {
            jfxButton(game.path.toString()) {
                addClass(Style.path)
                action { openFileActions.offer(game.path) }
            }
        }.apply {
            addClass(Style.fileTreeContainer)
            gridpaneConstraints { rowIndex = 1; columnIndex = 1; vAlignment = VPos.BOTTOM }
        }

        // Bottom Left
        flowpane {
            hgap = 15.0
            vgap = 15.0
            addClass(Style.screenshots)
            gridpaneConstraints { rowIndex = 2; columnIndex = 0 }

            game.screenshotUrls.forEach { url ->
                makeRoundCorners(imageview(commonOps.fetchImage(url, persist = true)) {
                    fitWidth = 100.0
                    fitHeight = 70.0
                }, arc = 10) {
                    scaleOnMouseOver(duration = 0.1.seconds, target = 1.15)

                    addClass(Style.screenshotItem)
                    setOnMouseClicked {
                        viewImageActions.offer(ViewImageParams(imageUrl = url, imageUrls = game.screenshotUrls))
                    }
                }
            }
        }

        // Bottom Right
        flowpane {
            hgap = 15.0
            vgap = 15.0
            addClass(Style.externalLinks)
            gridpaneConstraints { rowIndex = 2; columnIndex = 1 }

            label(graphic = Icons.youTube.size(70)) {
                minWidth = 100.0
                alignment = Pos.CENTER
                useMaxHeight = true
                addClass(Style.externalLinkItem)
                scaleOnMouseOver(duration = 0.1.seconds, target = 1.15)
                val url = commonOps.youTubeGameplayUrl(game)
                tooltip(url)
                setOnMouseClicked { browseUrlActions.offer(url) }
            }
            game.providerData.sortedBy { it.providerId }.forEach {
                val url = it.siteUrl
                label(graphic = commonOps.providerLogo(it.providerId).toImageView(height = 70, width = 100)) {
                    useMaxHeight = true
                    addClass(Style.externalLinkItem)
                    scaleOnMouseOver(duration = 0.1.seconds, target = 1.15)
                    tooltip(url)
                    setOnMouseClicked { browseUrlActions.offer(url) }
                }
            }
        }
    }

    private fun StackPane.createDate(createDate: JodaDateTime) = dateDisplay(createDate, Icons.createDate, Style.createDate, "Create Date", Pos.BOTTOM_RIGHT)
    private fun StackPane.updateDate(updateDate: JodaDateTime) = dateDisplay(updateDate, Icons.updateDate, Style.updateDate, "Update Date", Pos.BOTTOM_LEFT)

    private fun StackPane.dateDisplay(date: JodaDateTime, icon: FontIcon, styleClass: CssRule, tooltip: String, alignment: Pos) =
        label(date.defaultTimeZone.humanReadable, graphic = icon.size(16)) {
            addClass(styleClass)
            usePrefWidth = true
            tooltip(tooltip)
            stackpaneConstraints { this.alignment = alignment }
        }

    private fun FileTree.pathTo(target: FileTree): File? =
        if (this === target) {
            name.file
        } else {
            children.asSequence()
                .mapNotNull { it.pathTo(target) }
                .map { name.file.resolve(it) }
                .firstNotNull()
        }

    private fun EventTarget.icon(icon: FontIcon, size: Int = 16) =
        add(icon.size(size).apply {
            gridpaneConstraints { vAlignment = VPos.TOP; hAlignment = HPos.RIGHT }
        })

    init {
        // This view must call init manually because it is not created via 'inject'
        init()

        register()

        titleProperty.bind(game.stringBinding { it?.name })
    }

    class Style : Stylesheet() {
        companion object {
            val gameDetailsView by cssclass()
            val detailsViewContent by cssclass()
            val gameDetails by cssclass()
            val gameDetailsBackground by cssclass()
            val fileTreeContainer by cssclass()
            val externalLinks by cssclass()
            val screenshots by cssclass()
            val name by cssclass()
            val releaseDate by cssclass()
            val descriptionText by cssclass()
            val genreItem by cssclass()
            val tag by cssclass()
            val fileTree by cssclass()
            val path by cssclass()
            val screenshotItem by cssclass()
            val externalLinkItem by cssclass()
            val createDate by cssclass()
            val updateDate by cssclass()
            val arrowLeftContainer by cssclass()
            val arrowLeft by cssclass()
            val arrowRightContainer by cssclass()
            val arrowRight by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            gameDetailsView {
            }
            detailsViewContent {
                padding = box(40.px)
            }

            val semiTransparentBackground = mixin {
                padding = box(10.px)
                backgroundColor = multi(Colors.prettyLightGray.withOpacity(0.65))
                backgroundRadius = multi(box(10.px))
            }
            gameDetails {
                +semiTransparentBackground
            }
            gameDetailsBackground {
                opacity = 0.5
            }
            externalLinks {
                +semiTransparentBackground
                padding = box(20.px)
            }
            screenshots {
                +semiTransparentBackground
                padding = box(20.px)
            }
            fileTreeContainer {
                +semiTransparentBackground
            }
            name {
                wrapText = true
            }
            descriptionText {
                wrapText = true
            }
            genreItem {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Colors.niceBlue)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 2.px, horizontal = 8.px)
            }
            tag {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Colors.blueGrey)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 2.px, horizontal = 8.px)
            }
            fileTree {
                focusColor = Color.TRANSPARENT
                faintFocusColor = Color.TRANSPARENT
                maxHeight = 400.px
            }
            screenshotItem {
            }
            externalLinkItem {
            }
            createDate {
                fontSize = 12.px
            }
            updateDate {
                fontSize = 12.px
            }
            arrowLeftContainer {
                padding = box(20.px)
            }
            arrowLeft {
                backgroundColor = multi(Colors.transparentWhite)
                and(hover) {
                    backgroundColor = multi(Color.WHITE)
                }
            }
            arrowRightContainer {
                padding = box(20.px)
            }
            arrowRight {
                backgroundColor = multi(Colors.transparentWhite)
                and(hover) {
                    backgroundColor = multi(Color.WHITE)
                }
            }
        }
    }
}