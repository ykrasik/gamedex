package com.gitlab.ykrasik.gamedex.ui

import com.jfoenix.controls.JFXButton
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.paint.Color
import org.controlsfx.control.PopOver
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.addClass
import tornadofx.tooltip

/**
 * User: ykrasik
 * Date: 01/06/2017
 * Time: 20:37
 */
// TODO: There's probably a way to do this with css.
val toolbarGraphicSize = 26.0

fun acceptGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.CHECK_CIRCLE_ALT.toGraphic { size(size); color(Color.GREEN) }
fun cancelGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.BAN.toGraphic { size(size); color(Color.RED) }
fun backGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.UNDO.toGraphic { size(size); color(Color.GREEN) }
fun clearGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.TIMES_CIRCLE.toGraphic { size(size) }
fun extraGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.ELLIPSIS_V.toGraphic { size(size) }
fun addGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.PLUS.toGraphic { size(size); color(Color.GREEN) }
fun deleteGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.TRASH.toGraphic { size(size); color(Color.INDIANRED) }
fun editGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.PENCIL.toGraphic { size(size); color(Color.ORANGE) }
fun viewGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.EYE.toGraphic { size(size) }
fun thumbnailGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.FILE_IMAGE_ALT.toGraphic { size(size) }
fun tagGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.TAG.toGraphic { size(size); color(Color.BLUEVIOLET) }
fun searchGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.SEARCH.toGraphic { size(size); color(Color.DARKGOLDENROD) }
fun refreshGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.REFRESH.toGraphic { size(size); color(Color.DARKCYAN) }
fun reportGraphic(size: Double = toolbarGraphicSize) = FontAwesome.Glyph.BOOK.toGraphic { size(size) }

fun EventTarget.toolbarButton(text: String? = null, graphic: Node? = null, op: (JFXButton.() -> Unit)? = null) =
    jfxButton(text, graphic) {
        addClass(CommonStyle.toolbarButton)
        op?.invoke(this)
    }

fun EventTarget.acceptButton(size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(graphic = acceptGraphic(size)) {
        addClass(CommonStyle.acceptButton)
        tooltip("Accept")
        op?.invoke(this)
    }

fun EventTarget.cancelButton(size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(graphic = cancelGraphic(size)) {
        addClass(CommonStyle.cancelButton)
        tooltip("Cancel")
        op?.invoke(this)
    }

fun EventTarget.addButton(size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(graphic = addGraphic(size)) {
        addClass(CommonStyle.acceptButton)
        tooltip("Add")
        op?.invoke(this)
    }

fun EventTarget.deleteButton(text: String? = null, size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, deleteGraphic(size)) {
        addClass(CommonStyle.deleteButton)
        tooltip("Delete")
        op?.invoke(this)
    }

fun EventTarget.backButton(text: String? = null, size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, backGraphic(size)) {
        addClass(CommonStyle.acceptButton)
        isCancelButton = true
        tooltip("Back")
        op?.invoke(this)
    }

fun EventTarget.editButton(size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton("Edit", editGraphic(size), op)

fun EventTarget.tagButton(size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton("Tag", tagGraphic(size), op)

fun EventTarget.searchButton(text: String? = "Search", size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, searchGraphic(size), op)

fun EventTarget.refreshButton(text: String? = "Refresh", size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, refreshGraphic(size), op)

fun EventTarget.reportButton(text: String? = "Report", size: Double = toolbarGraphicSize, op: (JFXButton.() -> Unit)? = null) =
    toolbarButton(text, reportGraphic(size), op)


fun EventTarget.extraMenu(size: Double = toolbarGraphicSize, op: (PopOver.() -> Unit)? = null) = buttonWithPopover(
    graphic = extraGraphic(size),
    arrowLocation = PopOver.ArrowLocation.TOP_RIGHT,
    op = op
)