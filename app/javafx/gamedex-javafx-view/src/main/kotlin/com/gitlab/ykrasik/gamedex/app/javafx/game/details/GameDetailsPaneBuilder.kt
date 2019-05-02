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

package com.gitlab.ykrasik.gamedex.app.javafx.game.details

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.app.javafx.common.JavaFxCommonOps
import com.gitlab.ykrasik.gamedex.app.javafx.image.JavaFxImage
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.importStylesheetSafe
import com.gitlab.ykrasik.gamedex.javafx.theme.*
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.*
import com.jfoenix.controls.JFXButton
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import kotlinx.coroutines.channels.Channel
import org.kordamp.ikonli.javafx.FontIcon
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 16/01/2019
 * Time: 08:38
 */
class GameDetailsPaneBuilder(
    var name: String? = null,
    var nameOp: (Label.() -> Unit)? = null,

    var platform: Platform? = null,

    var description: String? = null,
    var descriptionOp: (Label.() -> Unit)? = null,

    var releaseDate: String? = null,
    var releaseDateOp: (Label.() -> Unit)? = null,

    var criticScore: Score? = null,
    var criticScoreOp: (VBox.() -> Unit)? = null,

    var userScore: Score? = null,
    var userScoreOp: (VBox.() -> Unit)? = null,

    var genres: List<String> = emptyList(),
    var genreOp: (Label.() -> Unit)? = null,

    var tags: List<String> = emptyList(),
    var tagOp: (Label.() -> Unit)? = null,

    var reportTags: List<String> = emptyList(),
    var reportTagOp: (Label.() -> Unit)? = null,

    var createDate: JodaDateTime? = null,
    var createDateOp: (Label.() -> Unit)? = null,

    var updateDate: JodaDateTime? = null,
    var updateDateOp: (Label.() -> Unit)? = null,

    var providerUrls: List<Pair<ProviderId, String>> = emptyList(),
    var providerLogos: Map<ProviderId, JavaFxImage> = emptyMap(),
    var browseUrlActions: Channel<String>? = null,

    var path: File? = null,
    var pathOp: (JFXButton.() -> Unit)? = null,
    var fileTree: Ref<FileTree?>? = null,
    var fileTreeOp: (Label.() -> Unit)? = null,
    var browsePathActions: Channel<File>? = null,

    var image: ObservableValue<Image>? = null,
    var imageFitHeight: Number = 200,
    var imageFitWidth: Number = 200,
    var imageOp: (VBox.() -> Unit)? = null,

    var orientation: Orientation = Orientation.VERTICAL,
    var fillWidth: Boolean = true
) {
    fun build(op: (HBox.() -> Unit)? = null) = HBox(5.0).apply {
        image?.let { image ->
            vbox {
                minWidth = imageFitWidth.toDouble()
                vbox {
                    alignment = Pos.TOP_RIGHT
                    imageview(image) {
                        fitWidth = imageFitWidth.toDouble()
                        fitHeight = imageFitHeight.toDouble()
                        isPreserveRatio = true
                    }
                    clipRectangle(arc = 10)
                    imageOp?.invoke(this)
                }
                tooltip("Thumbnail")
            }
        }
        gridpane {
            hgap = 5.0
            vgap = 5.0
            alignment = Pos.TOP_RIGHT
            name?.let { name ->
                row {
                    children += platform?.logo ?: Region()
                    header(name) {
                        addClass(Style.name)
                        tooltip("Name")
                        nameOp?.invoke(this)
                    }
                }
            }
            releaseDate?.let { releaseDate ->
                row {
                    children += Icons.date.size(16)
                    label(releaseDate) {
                        addClass(Style.releaseDate)
                        tooltip("Release Date")
                        releaseDateOp?.invoke(this)
                    }
                }
            }
            description?.let { description ->
                row {
                    region()
                    label(description) {
                        addClass(Style.descriptionText)
                        descriptionOp?.invoke(this)
                        tooltip("Description")
                    }
                }
            }
            if (genres.isNotEmpty()) {
                row {
                    children += Icons.masks.size(16)
                    flowpane {
                        hgap = 5.0
                        vgap = 3.0
                        tooltip("Genres")
                        genres.forEach {
                            label(it) {
                                addClass(Style.genreItem)
                                genreOp?.invoke(this)
                            }
                        }
                    }
                }
            }
            if (tags.isNotEmpty()) {
                row {
                    children += Icons.tag.size(16).color(Color.BLACK)
                    flowpane {
                        hgap = 5.0
                        vgap = 3.0
                        tooltip("Tags")
                        tags.forEach {
                            label(it) {
                                addClass(Style.tag)
                                tagOp?.invoke(this)
                            }
                        }
                    }
                }
            }
            if (reportTags.isNotEmpty()) {
                row {
                    children += Icons.tag.size(16).color(Color.BLACK)
                    flowpane {
                        hgap = 5.0
                        vgap = 3.0
                        tooltip("Tags generated by reports")
                        reportTags.forEach {
                            label(it) {
                                addClass(Style.tag)
                                reportTagOp?.invoke(this)
                            }
                        }
                    }
                }
            }
            if (providerUrls.isNotEmpty()) {
                row {
                    children += Icons.web.size(16)
                    vbox(spacing = 5) {
                        providerUrls.sortedBy { it.first }.forEach { (providerId, url) ->
                            defaultHbox {
                                stackpane {
                                    minWidth = 70.0
                                    alignment = Pos.CENTER_LEFT
                                    children += providerLogos.getValue(providerId).toImageView(height = 30, width = 70)
                                }
                                if (browseUrlActions != null) {
                                    hyperlink(url) {
                                        setId(Style.providerUrl)
                                        action { browseUrlActions!!.offer(url) }
                                    }
                                } else {
                                    label(url) {
                                        setId(Style.providerUrl)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            path?.let { path ->
                row {
                    children += Icons.folder.size(16)
                    defaultHbox {
                        jfxButton(path.toString()) {
                            addClass(Style.path)
                            isFocusTraversable = false
                            browsePathActions?.let { browsePathActions ->
                                setOnMouseClicked { browsePathActions.offer(path) }
                            }
                            tooltip("Browse to Path")
                            pathOp?.invoke(this)
                        }
                        fileTree?.value?.let { fileTree ->
                            spacer()
                            buttonWithPopover(
                                text = fileTree.size.humanReadable,
                                graphic = Icons.fileTree.size(18),
                                closeOnAction = false
                            ) {
                                fileTreeView(fileTree) {
                                    addClass(Style.fileTree)
                                    minWidth = 600.0
                                }
                            }.apply {
                                addClass(Style.sizeTaken)
                            }
                        }
                    }
                }
            }
        }
        if (orientation == Orientation.VERTICAL) {
            vbox(spacing = 5) {
                alignment = Pos.TOP_RIGHT
                if (fillWidth) {
                    hgrow = Priority.ALWAYS
                }
                vbox(spacing = 5) {
                    isFillWidth = false
                    alignment = Pos.TOP_RIGHT
                    criticScoreDisplay(criticScore)
                    userScoreDisplay(userScore)
                }
                createDate(createDate)
                updateDate(updateDate)
            }
        } else {
            defaultHbox(alignment = Pos.TOP_RIGHT) {
                isFillHeight = false
                if (fillWidth) {
                    hgrow = Priority.ALWAYS
                }
                criticScoreDisplay(criticScore)
                userScoreDisplay(userScore)
                vbox(spacing = 5) {
                    createDate(createDate)
                    updateDate(updateDate)
                }
            }
        }
        op?.invoke(this)
    }

    private fun EventTarget.createDate(createDate: JodaDateTime?) = dateDisplay(createDate, Icons.createDate, Style.createDate, "Create Date", createDateOp)
    private fun EventTarget.updateDate(updateDate: JodaDateTime?) = dateDisplay(updateDate, Icons.updateDate, Style.updateDate, "Update Date", updateDateOp)

    private fun EventTarget.dateDisplay(date: JodaDateTime?, icon: FontIcon, styleClass: CssRule, tooltip: String, op: (Label.() -> Unit)?) =
        date?.let {
            label(it.defaultTimeZone.humanReadable, graphic = icon.size(16)) {
                addClass(styleClass)
                tooltip(tooltip)
                op?.invoke(this)
            }
        }

    private fun EventTarget.criticScoreDisplay(score: Score?) = scoreDisplay(score, "Critic", Style.criticScore, Style.criticScoreReviews, criticScoreOp)
    private fun EventTarget.userScoreDisplay(score: Score?) = scoreDisplay(score, "User", Style.userScore, Style.userScoreReviews, userScoreOp)

    private fun EventTarget.scoreDisplay(
        score: Score?,
        name: String,
        scoreStyleClass: CssRule,
        reviewsStyleClass: CssRule,
        op: (VBox.() -> Unit)?
    ) = vbox {
        alignment = Pos.TOP_CENTER
        paddingAll = 5
        minWidth = 60.0
        clipRectangle(arc = 10)

        background = Background(BackgroundFill(score.ratingColor, CornerRadii.EMPTY, Insets.EMPTY))
        label(score?.score?.toString(decimalDigits = 1) ?: "N/A") { setId(scoreStyleClass) }
        label("${score?.numReviews ?: 0} ${name}s") { setId(reviewsStyleClass) }
        tooltip("$name Score")
        op?.invoke(this)
    }

    companion object {
        inline operator fun invoke(game: Game, commonOps: JavaFxCommonOps, op: GameDetailsPaneBuilder.() -> Unit) = GameDetailsPaneBuilder(
            name = game.name,
            platform = game.platform,
            description = game.description,
            releaseDate = game.releaseDate,
            criticScore = game.criticScore,
            userScore = game.userScore,
            genres = game.genres,
            tags = game.tags,
            reportTags = game.reportTags,
            createDate = game.createDate,
            updateDate = game.updateDate,

            providerUrls = game.rawGame.providerData.map { it.header.id to it.siteUrl },
            providerLogos = commonOps.providerLogos,

            path = game.path,
            fileTree = game.fileTree,

            image = commonOps.fetchThumbnail(game)
        ).also(op)
    }

    class Style : Stylesheet() {
        companion object {
            val name by cssclass()
            val path by cssclass()
            val sizeTaken by cssclass()
            val descriptionText by cssclass()
            val genreItem by cssclass()
            val tag by cssclass()
            val releaseDate by cssclass()
            val createDate by cssclass()
            val updateDate by cssclass()
            val providerUrl by cssid()
            val criticScore by cssid()
            val criticScoreReviews by cssid()
            val userScore by cssid()
            val userScoreReviews by cssid()
            val fileTree by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            path {
//                padding = box(4.px, 8.px, 4.px, 8.px)
//                backgroundRadius = multi(box(3.px))
            }
            descriptionText {
                wrapText = true
//                maxWidth = 800.px
            }

            genreItem {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Colors.anotherPrettyLightGray)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 2.px, horizontal = 8.px)
            }

            tag {
                borderRadius = multi(box(3.px))
                backgroundColor = multi(Colors.blueGrey)
                backgroundRadius = multi(box(3.px))
                padding = box(vertical = 2.px, horizontal = 8.px)
            }

            createDate {
                fontSize = 12.px
            }

            updateDate {
                fontSize = 12.px
            }

            providerUrl {
                textFill = Color.MEDIUMBLUE
            }

            criticScore {
                fontSize = 26.px
                fontWeight = FontWeight.BOLD
                textFill = Color.WHITE
            }

            criticScoreReviews {
                fontSize = 18.px
                textFill = Color.WHITE
            }

            userScore {
                fontSize = 18.px
                fontWeight = FontWeight.BOLD
                textFill = Color.WHITE
            }

            userScoreReviews {
                fontSize = 15.px
                textFill = Color.WHITE
            }

            fileTree {
                focusColor = Color.TRANSPARENT
                faintFocusColor = Color.TRANSPARENT
//                backgroundColor = multi(Colors.cloudyKnoxville)
            }
        }
    }
}
