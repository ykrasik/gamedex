/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.javafx.Theme.Icon.defaultIconSize
import com.gitlab.ykrasik.gamedex.util.browse
import com.jfoenix.controls.JFXButton
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.controlsfx.control.PopOver
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.addClass
import tornadofx.label
import tornadofx.toProperty
import tornadofx.tooltip
import java.io.File

/**
 * User: ykrasik
 * Date: 02/06/2017
 * Time: 18:42
 */
object Theme {
    object Icon {
        // TODO: There's probably a way to do this with css.
        val defaultIconSize = 26.0

        fun accept(size: Double = defaultIconSize) = FontAwesome.Glyph.CHECK_CIRCLE_ALT.toGraphic { size(size); color(Color.GREEN) }
        fun cancel(size: Double = defaultIconSize) = FontAwesome.Glyph.BAN.toGraphic { size(size); color(Color.RED) }
        fun back(size: Double = defaultIconSize) = FontAwesome.Glyph.UNDO.toGraphic { size(size); color(Color.GREEN) }
        fun clear(size: Double = defaultIconSize) = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(size) }
        fun extra(size: Double = defaultIconSize) = FontAwesome.Glyph.ELLIPSIS_V.toGraphic { size(size) }

        fun plus(size: Double = defaultIconSize) = FontAwesome.Glyph.PLUS.toGraphic { size(size); color(Color.GREEN) }
        fun minus(size: Double = defaultIconSize) = FontAwesome.Glyph.MINUS.toGraphic { size(size); color(Color.INDIANRED) }
        fun arrowLeft(size: Double = defaultIconSize) = FontAwesome.Glyph.ARROW_LEFT.toGraphic { size(size) }
        fun arrowRight(size: Double = defaultIconSize) = FontAwesome.Glyph.ARROW_RIGHT.toGraphic { size(size) }
        fun maximize(size: Double = defaultIconSize) = FontAwesome.Glyph.ARROWS_ALT.toGraphic { size(size) }
        fun ascending(size: Double = defaultIconSize) = FontAwesome.Glyph.SORT_ASC.toGraphic { size(size) }
        fun descending(size: Double = defaultIconSize) = FontAwesome.Glyph.SORT_DESC.toGraphic { size(size) }

        fun delete(size: Double = defaultIconSize, color: Color = Color.INDIANRED) = FontAwesome.Glyph.TRASH.toGraphic { size(size); color(color) }
        fun edit(size: Double = defaultIconSize) = FontAwesome.Glyph.PENCIL.toGraphic { size(size); color(Color.ORANGE) }
        fun view(size: Double = defaultIconSize) = FontAwesome.Glyph.EYE.toGraphic { size(size) }
        fun not(size: Double = defaultIconSize) = FontAwesome.Glyph.EXCLAMATION.toGraphic { size(size); color(Color.MEDIUMVIOLETRED) }
        fun checked(size: Double = defaultIconSize) = FontAwesome.Glyph.CHECK_CIRCLE_ALT.toGraphic { size(size) }
        fun unchecked(size: Double = defaultIconSize) = FontAwesome.Glyph.CIRCLE_ALT.toGraphic { size(size) }

        fun thumbnail(size: Double = defaultIconSize) = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic { size(size) }
        fun poster(size: Double = defaultIconSize) = FontAwesome.Glyph.PICTURE_ALT.toGraphic { size(size) }
        fun tag(size: Double = defaultIconSize) = FontAwesome.Glyph.TAG.toGraphic { size(size); color(Color.BLUEVIOLET) }

        fun search(size: Double = defaultIconSize) = FontAwesome.Glyph.SEARCH.toGraphic { size(size); color(Color.DARKGOLDENROD) }
        fun refresh(size: Double = defaultIconSize) = FontAwesome.Glyph.REFRESH.toGraphic { size(size); color(Color.DARKCYAN) }
        fun chart(size: Double = defaultIconSize) = FontAwesome.Glyph.BAR_CHART.toGraphic { size(size); color(Color.DARKBLUE) }
        fun book(size: Double = defaultIconSize) = FontAwesome.Glyph.BOOK.toGraphic { size(size); color(Color.BURLYWOOD) }
        fun filter(size: Double = defaultIconSize) = FontAwesome.Glyph.FILTER.toGraphic { size(size) }
        fun folder(size: Double = defaultIconSize) = FontAwesome.Glyph.FOLDER.toGraphic { size(size) }
        fun folderOpen(size: Double = defaultIconSize) = FontAwesome.Glyph.FOLDER_OPEN.toGraphic { size(size) }

        fun download(size: Double = defaultIconSize) = FontAwesome.Glyph.DOWNLOAD.toGraphic { size(size) }
        fun upload(size: Double = defaultIconSize) = FontAwesome.Glyph.UPLOAD.toGraphic { size(size) }

        fun windows(size: Double = defaultIconSize) = FontAwesome.Glyph.WINDOWS.toGraphic { size(size); color(Color.CORNFLOWERBLUE) }
        fun android(size: Double = defaultIconSize) = FontAwesome.Glyph.ANDROID.toGraphic { size(size); color(Color.FORESTGREEN) }
        fun apple(size: Double = defaultIconSize) = FontAwesome.Glyph.APPLE.toGraphic { size(size); color(Color.GRAY) }

        fun question(size: Double = defaultIconSize) = FontAwesome.Glyph.QUESTION.toGraphic { size(size); color(Color.LIGHTGREEN) }
        fun exclamationTriangle(size: Double = defaultIconSize) = FontAwesome.Glyph.EXCLAMATION_TRIANGLE.toGraphic { size(size) }
        fun timesCircle(size: Double = defaultIconSize) = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(size) }

        fun bars(size: Double = defaultIconSize) = FontAwesome.Glyph.BARS.toGraphic { size(size) }
        fun games(size: Double = defaultIconSize) = FontAwesome.Glyph.GAMEPAD.toGraphic { size(size); color(Color.DARKRED) }
        fun hdd(size: Double = defaultIconSize) = FontAwesome.Glyph.HDD_ALT.toGraphic { size(size); color(Color.DARKGREEN) }
        fun settings(size: Double = defaultIconSize) = FontAwesome.Glyph.COG.toGraphic { size(size); color(Color.GRAY) }
        fun quit(size: Double = defaultIconSize) = FontAwesome.Glyph.SIGN_OUT.toGraphic { size(size) }
    }

    object Images {
        val confirm = Image(javaClass.getResourceAsStream("confirm.png"))
        val information = Image(javaClass.getResourceAsStream("information.png"))
        val warning = Image(javaClass.getResourceAsStream("warning.png"))
        val error = Image(javaClass.getResourceAsStream("error.png"))
    }
}

inline fun EventTarget.toolbarButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    jfxButton(text, graphic) {
        addClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.acceptButton(size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(graphic = Theme.Icon.accept(size)) {
        addClass(CommonStyle.acceptButton)
        isDefaultButton = true
        tooltip("Accept")
        op()
    }

inline fun EventTarget.cancelButton(size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(graphic = Theme.Icon.cancel(size)) {
        addClass(CommonStyle.cancelButton)
        isCancelButton = true
        tooltip("Cancel")
        op()
    }

inline fun EventTarget.backButton(text: String? = null, size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.back(size)) {
        addClass(CommonStyle.acceptButton)
        isCancelButton = true
        tooltip("Back")
        op()
    }

inline fun EventTarget.addButton(size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(graphic = Theme.Icon.plus(size)) {
        addClass(CommonStyle.acceptButton)
        tooltip("Add")
        op()
    }

inline fun EventTarget.deleteButton(text: String? = null, size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.delete(size)) {
        addClass(CommonStyle.deleteButton)
        tooltip("Delete")
        op()
    }

inline fun EventTarget.excludeButton(text: String = "Exclude", size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.exclamationTriangle(size)) {
        addClass(CommonStyle.excludeButton)
        tooltip(text)
        op()
    }

inline fun EventTarget.editButton(size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton("Edit", Theme.Icon.edit(size), op)

inline fun EventTarget.tagButton(size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton("Tag", Theme.Icon.tag(size), op)

inline fun EventTarget.searchButton(text: String, size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.search(size), op)

inline fun EventTarget.refreshButton(text: String? = "Refresh", size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.refresh(size), op)

inline fun EventTarget.downloadButton(text: String? = "Download", size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.download(size), op)

inline fun EventTarget.reportButton(text: String? = "Report", size: Double = defaultIconSize, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Theme.Icon.chart(size), op)


inline fun EventTarget.extraMenu(size: Double = defaultIconSize, op: VBox.(PopOver) -> Unit = {}) = buttonWithPopover(
    graphic = Theme.Icon.extra(size),
    arrowLocation = PopOver.ArrowLocation.TOP_RIGHT,
    op = op
)

fun Platform.toLogo(size: Double = 19.0) = when (this) {
    Platform.pc -> Theme.Icon.windows(size)
    Platform.android -> Theme.Icon.android(size)
    Platform.mac -> Theme.Icon.apple(size)
    Platform.excluded -> Theme.Icon.cancel(size)
    else -> Theme.Icon.question(size)
}.apply {
    minWidth = Region.USE_PREF_SIZE
}

fun EventTarget.platformComboBox(selected: Property<Platform>) = popoverComboMenu(
    possibleItems = Platform.values().toList(),
    selectedItemProperty = selected,
    styleClass = CommonStyle.platformItem,
    itemStyleClass = CommonStyle.fillAvailableWidth,
    text = Platform::displayName,
    graphic = { it.toLogo() }
)

@Deprecated("Delegate to presenter for this.")
inline fun EventTarget.pathButton(path: File, op: JFXButton.() -> Unit = {}) = jfxButton(path.path) {
    isFocusTraversable = false
    setOnAction { browse(path) }
    op(this)
}

inline fun EventTarget.header(text: String, crossinline op: Label.() -> Unit = {}) = header(text.toProperty(), op)
inline fun EventTarget.header(textProperty: ObservableValue<String>, crossinline op: Label.() -> Unit = {}) = label(textProperty) {
    addClass(CommonStyle.headerLabel)
    op(this)
}

fun Any?.toDisplayString() = this?.toString() ?: "NA"