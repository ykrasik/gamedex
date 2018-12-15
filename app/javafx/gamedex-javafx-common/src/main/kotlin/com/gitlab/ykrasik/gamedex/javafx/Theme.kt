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
import com.gitlab.ykrasik.gamedex.javafx.control.buttonWithPopover
import com.gitlab.ykrasik.gamedex.javafx.control.jfxButton
import com.gitlab.ykrasik.gamedex.util.browse
import com.jfoenix.controls.JFXButton
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.controlsfx.control.PopOver
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 02/06/2017
 * Time: 18:42
 */
object Icons {
    private val red = Color.DARKRED.brighter()
    private val green = Color.DARKGREEN
    private val orange = Color.ORANGE.darker()
    private val blue = Color.CORNFLOWERBLUE

    val menu get() = fontIcon(MaterialDesign.MDI_MENU)
    val games get() = fontIcon(MaterialDesign.MDI_GAMEPAD_VARIANT, color = red)
    val chart get() = fontIcon(MaterialDesign.MDI_CHART_BAR, color = Color.DARKBLUE)
    val book get() = fontIcon(MaterialDesign.MDI_BOOK_OPEN_OUTLINE, color = Color.BROWN)
    val hdd get() = fontIcon(MaterialDesign.MDI_HARDDISK, color = green)
    val settings get() = fontIcon(MaterialDesign.MDI_SETTINGS, color = Color.GRAY)
    val wrench get() = fontIcon(MaterialDesign.MDI_WRENCH, color = Color.GRAY)
    val display get() = fontIcon(MaterialDesign.MDI_MONITOR_DASHBOARD, color = Color.DARKBLUE)
    val grid get() = fontIcon(MaterialDesign.MDI_GRID)
    val quit get() = fontIcon(MaterialDesign.MDI_LOGOUT)

    val accept get() = fontIcon(MaterialDesign.MDI_CHECK_CIRCLE, color = green)
    val cancel get() = fontIcon(MaterialDesign.MDI_CLOSE_CIRCLE, color = red)
    val close get() = fontIcon(MaterialDesign.MDI_CLOSE, color = red)
    val stop get() = fontIcon(MaterialDesign.MDI_STOP, color = red)
    val redo get() = fontIcon(MaterialDesign.MDI_REDO, color = orange)
    val clear get() = fontIcon(MaterialDesign.MDI_CLOSE_CIRCLE_OUTLINE)
    val resetToDefault get() = fontIcon(MaterialDesign.MDI_RESTORE)
    val dots get() = fontIcon(MaterialDesign.MDI_DOTS_VERTICAL)

    val check get() = fontIcon(MaterialDesign.MDI_CHECK, color = green)
    val valid get() = fontIcon(MaterialDesign.MDI_SHIELD_CHECK, color = green)
    val invalid get() = fontIcon(MaterialDesign.MDI_SHIELD_OFF, color = red)
    val unverified get() = fontIcon(MaterialDesign.MDI_SHIELD_OUTLINE, color = orange)
    val excluded get() = fontIcon(MaterialDesign.MDI_CLOSE_OUTLINE, color = red)
    val checked get() = fontIcon(MaterialDesign.MDI_CHECKBOX_MARKED_OUTLINE, color = green)
    val checkX get() = fontIcon(MaterialDesign.MDI_CLOSE_BOX_OUTLINE, color = red)

    val add get() = fontIcon(MaterialDesign.MDI_PLUS_CIRCLE, color = green)
    val plus get() = fontIcon(MaterialDesign.MDI_PLUS, color = green)
    val minus get() = fontIcon(MaterialDesign.MDI_MINUS, color = red)
    val arrowLeft get() = fontIcon(MaterialDesign.MDI_ARROW_LEFT)
    val arrowRight get() = fontIcon(MaterialDesign.MDI_ARROW_RIGHT)
    val chevronRight get() = fontIcon(MaterialDesign.MDI_CHEVRON_RIGHT)

    val tune get() = fontIcon(MaterialDesign.MDI_TUNE_VERTICAL)
    val filter get() = fontIcon(MaterialDesign.MDI_FILTER)
    val sort get() = fontIcon(MaterialDesign.MDI_SORT)
    val sortAlphabetical get() = fontIcon(MaterialDesign.MDI_SORT_ALPHABETICAL)
    val ascending get() = fontIcon(MaterialDesign.MDI_SORT_ASCENDING)
    val descending get() = fontIcon(MaterialDesign.MDI_SORT_DESCENDING)

    val fullscreen get() = fontIcon(MaterialDesign.MDI_FULLSCREEN)
    val exitFullscreen get() = fontIcon(MaterialDesign.MDI_FULLSCREEN_EXIT)

    val view get() = fontIcon(MaterialDesign.MDI_EYE_OUTLINE)
    val details get() = fontIcon(MaterialDesign.MDI_FILE_FIND_OUTLINE)
    val edit get() = fontIcon(MaterialDesign.MDI_PENCIL, color = orange)
    val delete get() = fontIcon(MaterialDesign.MDI_DELETE_FOREVER, color = red)
    val deleteSolid get() = fontIcon(MaterialDesign.MDI_DELETE, color = red)

    val tag get() = fontIcon(MaterialDesign.MDI_TAG, color = Color.BLUEVIOLET)
    val search get() = fontIcon(MaterialDesign.MDI_MAGNIFY, color = blue)
    val sync get() = fontIcon(MaterialDesign.MDI_SYNC, color = green)

    val text get() = fontIcon(MaterialDesign.MDI_FORMAT_COLOR_TEXT)
    val enterText get() = fontIcon(MaterialDesign.MDI_RENAME_BOX, color = orange)
    val textbox get() = fontIcon(MaterialDesign.MDI_TEXTBOX)
    val documents get() = fontIcon(MaterialDesign.MDI_BOOK_MULTIPLE_VARIANT)
    val date get() = fontIcon(MaterialDesign.MDI_CALENDAR)
    val createDate get() = fontIcon(MaterialDesign.MDI_CALENDAR_PLUS)
    val updateDate get() = fontIcon(MaterialDesign.MDI_CALENDAR_EDIT)
    val thumbnail get() = fontIcon(MaterialDesign.MDI_FILE_IMAGE)
    val poster get() = fontIcon(MaterialDesign.MDI_IMAGE)
    val screenshots get() = fontIcon(MaterialDesign.MDI_IMAGE_MULTIPLE)
    val duplicate get() = fontIcon(MaterialDesign.MDI_CONTENT_DUPLICATE)
    val diff get() = fontIcon(MaterialDesign.MDI_VECTOR_DIFFERENCE)
    val select get() = fontIcon(MaterialDesign.MDI_SELECT)

    val folder get() = fontIcon(MaterialDesign.MDI_FOLDER_OUTLINE)
    val folderFilled get() = fontIcon(MaterialDesign.MDI_FOLDER)
    val folderEdit get() = fontIcon(MaterialDesign.MDI_FOLDER_EDIT, color = orange)
    val folderRemove get() = fontIcon(MaterialDesign.MDI_FOLDER_REMOVE_OUTLINE)
    val folderOpen get() = fontIcon(MaterialDesign.MDI_FOLDER_OPEN)
    val folderSearch get() = fontIcon(MaterialDesign.MDI_FOLDER_SEARCH_OUTLINE)
    val fileTree get() = fontIcon(MaterialDesign.MDI_FILE_TREE)
    val fileQuestion get() = fontIcon(MaterialDesign.MDI_FILE_QUESTION)

    val download get() = fontIcon(MaterialDesign.MDI_CLOUD_DOWNLOAD)
    val upload get() = fontIcon(MaterialDesign.MDI_CLOUD_UPLOAD)
    val export get() = fontIcon(MaterialDesign.MDI_EXPORT)
    val import get() = fontIcon(MaterialDesign.MDI_IMPORT)
    val database get() = fontIcon(MaterialDesign.MDI_DATABASE)
    val addNetwork get() = fontIcon(MaterialDesign.MDI_PLUS_NETWORK)
    val siteMap get() = fontIcon(MaterialDesign.MDI_SITEMAP)
    val tournament get() = fontIcon(MaterialDesign.MDI_TOURNAMENT)
    val history get() = fontIcon(MaterialDesign.MDI_HISTORY)
    val clockStart get() = fontIcon(MaterialDesign.MDI_CLOCK_START)
    val clockEnd get() = fontIcon(MaterialDesign.MDI_CLOCK_END)
    val databaseCleanup
        get() = StackPane().apply {
            add(Icons.database)
            add(Icons.deleteSolid.apply {
                size(15)
                color(Color.ORANGERED)
                stackpaneConstraints { alignment = Pos.BOTTOM_RIGHT }
            })
        }

    val computer get() = fontIcon(MaterialDesign.MDI_DESKTOP_CLASSIC)
    val windows get() = fontIcon(MaterialDesign.MDI_WINDOWS, Color.CORNFLOWERBLUE)
    val android get() = fontIcon(MaterialDesign.MDI_ANDROID, Color.FORESTGREEN)
    val apple get() = fontIcon(MaterialDesign.MDI_APPLE, Color.GRAY)

    val information get() = fontIcon(MaterialDesign.MDI_INFORMATION, color = Color.CADETBLUE)
    val question get() = fontIcon(MaterialDesign.MDI_HELP_CIRCLE_OUTLINE, color = Color.YELLOWGREEN)
    val warning get() = fontIcon(MaterialDesign.MDI_ALERT_OUTLINE, color = Color.ORANGE)
    val error get() = fontIcon(MaterialDesign.MDI_CLOSE_BOX_OUTLINE, color = Color.INDIANRED)
    val exclamation get() = fontIcon(MaterialDesign.MDI_EXCLAMATION, color = Color.RED)
    val or get() = fontIcon(MaterialDesign.MDI_GATE_OR)
    val and get() = fontIcon(MaterialDesign.MDI_GATE_AND)
    val validationError get() = warning.color(Color.RED)

    val equal get() = fontIcon(MaterialDesign.MDI_EQUAL)
    val notEqual get() = fontIcon(MaterialDesign.MDI_NOT_EQUAL_VARIANT)
    val gtOrEq get() = fontIcon(MaterialDesign.MDI_GREATER_THAN_OR_EQUAL)
    val lt get() = fontIcon(MaterialDesign.MDI_LESS_THAN)
    val min get() = fontIcon(MaterialDesign.MDI_ARROW_COLLAPSE_DOWN)
    val max get() = fontIcon(MaterialDesign.MDI_CHART_BELL_CURVE)
    val contain get() = fontIcon(MaterialDesign.MDI_SET_LEFT_CENTER)
    val notContain get() = fontIcon(MaterialDesign.MDI_SET_LEFT)
    val `null` get() = fontIcon(MaterialDesign.MDI_NULL)

    val starFull get() = fontIcon(MaterialDesign.MDI_STAR)
    val starEmpty get() = fontIcon(MaterialDesign.MDI_STAR_OUTLINE)
    val starHalf get() = fontIcon(MaterialDesign.MDI_STAR_HALF)

    val logTrace get() = fontIcon(MaterialDesign.MDI_ALPHA_T_BOX, color = Color.LIGHTGRAY)
    val logDebug get() = fontIcon(MaterialDesign.MDI_ALPHA_D_BOX, color = Color.GRAY)
    val logInfo get() = fontIcon(MaterialDesign.MDI_ALPHA_I_BOX)
    val logWarn get() = fontIcon(MaterialDesign.MDI_ALPHA_W_BOX, color = orange)
    val logError get() = fontIcon(MaterialDesign.MDI_ALPHA_E_BOX, color = red)
}

fun fontIcon(icon: Ikon, color: Paint = Color.BLACK): FontIcon = FontIcon.of(icon, 30).color(color)
fun FontIcon.color(color: Paint): FontIcon = apply { iconColor = color }
fun FontIcon.size(size: Int): FontIcon = apply { iconSize = size }

inline fun EventTarget.toolbarButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    jfxButton(text, graphic) {
        addClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.confirmButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.confirmButton)
        op()
    }

inline fun EventTarget.warningButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.warningButton)
        op()
    }

inline fun EventTarget.dangerButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.dangerButton)
        op()
    }

inline fun EventTarget.infoButton(text: String? = null, graphic: Node? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, graphic) {
        addClass(CommonStyle.infoButton)
        op()
    }

inline fun EventTarget.acceptButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    confirmButton(text, Icons.accept) {
        isDefaultButton = true
        tooltip("Accept")
        op()
    }

inline fun EventTarget.cancelButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.cancel) {
        isCancelButton = true
        tooltip("Cancel")
        op()
    }

inline fun EventTarget.stopButton(text: String? = "Stop", crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.stop) {
        isCancelButton = true
        tooltip("Stop")
        op()
    }

inline fun EventTarget.backButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Icons.arrowLeft) {
        isCancelButton = true
        tooltip("Back")
        op()
    }

inline fun EventTarget.resetToDefaultButton(text: String? = "Reset to Default", crossinline op: JFXButton.() -> Unit = {}) =
    warningButton(text, Icons.resetToDefault) {
        tooltip("Reset to Defaults")
        op()
    }

inline fun EventTarget.addButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    confirmButton(text, Icons.add) {
        tooltip("Add")
        op()
    }

inline fun EventTarget.deleteButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.delete) {
        tooltip("Delete")
        op()
    }

inline fun EventTarget.plusButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    confirmButton(text, Icons.plus.size(22)) {
        removeClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.minusButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    dangerButton(text, Icons.minus.size(22)) {
        removeClass(CommonStyle.toolbarButton)
        op()
    }

inline fun EventTarget.excludeButton(text: String = "Exclude", crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Icons.warning) {
        addClass(CommonStyle.warningButton)
        tooltip(text)
        op()
    }

inline fun EventTarget.editButton(text: String? = null, crossinline op: JFXButton.() -> Unit = {}) =
    toolbarButton(text, Icons.edit, op)

inline fun EventTarget.syncButton(text: String, crossinline op: JFXButton.() -> Unit = {}) =
    infoButton(text, Icons.sync, op)

inline fun EventTarget.extraMenu(op: VBox.(PopOver) -> Unit = {}) = buttonWithPopover(
    graphic = Icons.dots,
    arrowLocation = PopOver.ArrowLocation.TOP_RIGHT,
    op = op
)

val Platform.logo
    get() = when (this) {
        Platform.pc -> Icons.windows
        Platform.android -> Icons.android
        Platform.mac -> Icons.apple
        Platform.excluded -> Icons.excluded
        else -> kotlin.error("Unknown platform: $this")
    }.size(26)

@Deprecated("Delegate to presenter for this.")
inline fun EventTarget.pathButton(path: File, op: JFXButton.() -> Unit = {}) = jfxButton(path.path) {
    isFocusTraversable = false
    action { browse(path) }
    op(this)
}

inline fun EventTarget.header(text: String, crossinline op: Label.() -> Unit = {}) = header(text.toProperty(), op)
inline fun EventTarget.header(textProperty: ObservableValue<String>, crossinline op: Label.() -> Unit = {}) = label(textProperty) {
    addClass(CommonStyle.headerLabel)
    op(this)
}

fun Any?.toDisplayString() = this?.toString() ?: "NA"