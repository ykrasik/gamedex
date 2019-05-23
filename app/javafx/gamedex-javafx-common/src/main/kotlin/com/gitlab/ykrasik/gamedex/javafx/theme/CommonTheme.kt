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

package com.gitlab.ykrasik.gamedex.javafx.theme

import com.gitlab.ykrasik.gamedex.LibraryType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.Score
import com.gitlab.ykrasik.gamedex.javafx.control.clipRectangle
import com.gitlab.ykrasik.gamedex.util.toString
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.VBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 05/01/2019
 * Time: 08:57
 */

val Platform.logo
    get() = when (this) {
        Platform.Windows -> Icons.windows
        Platform.Linux -> Icons.linux
        Platform.Android -> Icons.android
        Platform.Mac -> Icons.apple
    }.size(26)

val LibraryType.icon
    get() = when (this) {
        LibraryType.Digital -> Icons.folder
        LibraryType.Excluded -> Icons.folderRemove
    }.size(26)

inline fun EventTarget.header(
    text: String,
    graphic: Node? = null,
    crossinline op: Label.() -> Unit = {}
) = header(text.toProperty(), graphic?.toProperty(), op)

inline fun EventTarget.header(
    textProperty: ObservableValue<String>,
    graphic: ObservableValue<out Node>? = null,
    crossinline op: Label.() -> Unit = {}
) = label(textProperty) {
    addClass(GameDexStyle.headerLabel)
    if (graphic != null) this.graphicProperty().bind(graphic)
    op(this)
}

inline fun EventTarget.subHeader(
    text: String,
    graphic: Node? = null,
    crossinline op: Label.() -> Unit = {}
) = subHeader(text.toProperty(), graphic?.toProperty(), op)

inline fun EventTarget.subHeader(
    textProperty: ObservableValue<String>,
    graphic: ObservableValue<out Node>? = null,
    crossinline op: Label.() -> Unit = {}
) = label(textProperty) {
    addClass(GameDexStyle.subHeaderLabel)
    if (graphic != null) this.graphicProperty().bind(graphic)
    op(this)
}

fun EventTarget.criticScoreDisplay(score: Score?, op: (VBox.() -> Unit)? = null) =
    scoreDisplay(score, "Critic", GameDexStyle.criticScore, GameDexStyle.criticScoreReviews, op)

fun EventTarget.userScoreDisplay(score: Score?, op: (VBox.() -> Unit)? = null) =
    scoreDisplay(score, "User", GameDexStyle.userScore, GameDexStyle.userScoreReviews, op)

private fun EventTarget.scoreDisplay(
    score: Score?,
    name: String,
    scoreStyleClass: CssRule,
    reviewsStyleClass: CssRule,
    op: (VBox.() -> Unit)? = null
) = vbox {
    alignment = Pos.TOP_CENTER
    paddingAll = 5
    usePrefWidth = true
    clipRectangle(arc = 10)

    background = Background(BackgroundFill(score.ratingColor, CornerRadii.EMPTY, Insets.EMPTY))
    label(score?.score?.toString(decimalDigits = 1) ?: "N/A") { addClass(scoreStyleClass) }
    spacer()
    label("${score?.numReviews ?: 0} ${name}s") { addClass(reviewsStyleClass) }
    tooltip("$name Score")
    op?.invoke(this)
}
