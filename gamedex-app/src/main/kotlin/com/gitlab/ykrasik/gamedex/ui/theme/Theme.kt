package com.gitlab.ykrasik.gamedex.ui.theme

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ui.buttonWithPopover
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.theme.Theme.Icon.defaultIconSize
import com.gitlab.ykrasik.gamedex.ui.toGraphic
import com.gitlab.ykrasik.gamedex.util.toStringOr
import com.jfoenix.controls.JFXButton
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import org.controlsfx.control.PopOver
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.addClass
import tornadofx.tooltip
import java.awt.Desktop
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
        fun ascending(size: Double = defaultIconSize) = FontAwesome.Glyph.SORT_ASC.toGraphic { size(size) }
        fun descending(size: Double = defaultIconSize) = FontAwesome.Glyph.SORT_DESC.toGraphic { size(size) }

        fun delete(size: Double = defaultIconSize) = FontAwesome.Glyph.TRASH.toGraphic { size(size); color(Color.INDIANRED) }
        fun edit(size: Double = defaultIconSize) = FontAwesome.Glyph.PENCIL.toGraphic { size(size); color(Color.ORANGE) }
        fun view(size: Double = defaultIconSize) = FontAwesome.Glyph.EYE.toGraphic { size(size) }

        fun thumbnail(size: Double = defaultIconSize) = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic { size(size) }
        fun poster(size: Double = defaultIconSize) = FontAwesome.Glyph.PICTURE_ALT.toGraphic { size(size) }
        fun tag(size: Double = defaultIconSize) = FontAwesome.Glyph.TAG.toGraphic { size(size); color(Color.BLUEVIOLET) }

        fun search(size: Double = defaultIconSize) = FontAwesome.Glyph.SEARCH.toGraphic { size(size); color(Color.DARKGOLDENROD) }
        fun refresh(size: Double = defaultIconSize) = FontAwesome.Glyph.REFRESH.toGraphic { size(size); color(Color.DARKCYAN) }
        fun chart(size: Double = defaultIconSize) = FontAwesome.Glyph.BAR_CHART.toGraphic { size(size); color(Color.BURLYWOOD) }
        fun report(size: Double = defaultIconSize) = FontAwesome.Glyph.BOOK.toGraphic { size(size); color(Color.DARKBLUE) }
        fun filter(size: Double = defaultIconSize) = FontAwesome.Glyph.FILTER.toGraphic { size(size) }

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
        // TODO: Only used by ImageLoader, consider moving these resources there.
        val notAvailable = Image(javaClass.getResourceAsStream("no-image-available.png"))
        val loading = Image(javaClass.getResourceAsStream("spinner.gif"))

        val confirm = Image(javaClass.getResourceAsStream("confirm.png"))
        val information = Image(javaClass.getResourceAsStream("information.png"))
        val warning = Image(javaClass.getResourceAsStream("warning.png"))
        val error = Image(javaClass.getResourceAsStream("error.png"))
        val tick = Image(javaClass.getResourceAsStream("Green-Tick-PNG-Pic.png"))
    }
}

fun EventTarget.toolbarButton(text: String? = null, graphic: Node? = null, op: (JFXButton.() -> Unit)? = null) =
    jfxButton(text, graphic) {
        addClass(CommonStyle.toolbarButton)
        op?.invoke(this)
    }

fun EventTarget.acceptButton(size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(graphic = Theme.Icon.accept(size)) {
        addClass(CommonStyle.acceptButton)
        tooltip("Accept")
        op?.invoke(this)
    }

fun EventTarget.cancelButton(size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(graphic = Theme.Icon.cancel(size)) {
        addClass(CommonStyle.cancelButton)
        tooltip("Cancel")
        op?.invoke(this)
    }

fun EventTarget.backButton(text: String? = null, size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, Theme.Icon.back(size)) {
        addClass(CommonStyle.acceptButton)
        isCancelButton = true
        tooltip("Back")
        op?.invoke(this)
    }

fun EventTarget.addButton(size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(graphic = Theme.Icon.plus(size)) {
        addClass(CommonStyle.acceptButton)
        tooltip("Add")
        op?.invoke(this)
    }

fun EventTarget.deleteButton(text: String? = null, size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, Theme.Icon.delete(size)) {
        addClass(CommonStyle.deleteButton)
        tooltip("Delete")
        op?.invoke(this)
    }

fun EventTarget.editButton(size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton("Edit", Theme.Icon.edit(size), op)

fun EventTarget.tagButton(size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton("Tag", Theme.Icon.tag(size), op)

fun EventTarget.searchButton(text: String? = "Search", size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, Theme.Icon.search(size), op)

fun EventTarget.refreshButton(text: String? = "Refresh", size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, Theme.Icon.refresh(size), op)

fun EventTarget.reportButton(text: String? = "Report", size: Double = defaultIconSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, Theme.Icon.report(size), op)


fun EventTarget.extraMenu(size: Double = defaultIconSize, op: (VBox.() -> Unit)? = null) = buttonWithPopover(
    graphic = Theme.Icon.extra(size),
    arrowLocation = PopOver.ArrowLocation.TOP_RIGHT,
    op = op
)

fun Platform.toLogo(size: Double = 19.0) = when (this) {
    Platform.pc -> Theme.Icon.windows(size)
    Platform.android -> Theme.Icon.android(size)
    Platform.mac -> Theme.Icon.apple(size)
    else -> Theme.Icon.question(size)
}.apply {
    minWidth = Region.USE_PREF_SIZE
}

fun EventTarget.pathButton(path: File, op: (JFXButton.() -> Unit)? = null) = jfxButton(path.path) {
    isFocusTraversable = false
    setOnAction { Desktop.getDesktop().open(path) }
    op?.invoke(this)
}

fun Any?.toDisplayString() = toStringOr("NA")