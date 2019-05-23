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

import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign.MaterialDesign

/**
 * User: ykrasik
 * Date: 02/06/2017
 * Time: 18:42
 */
object Icons {
    val menu get() = fontIcon(MaterialDesign.MDI_MENU)
    val games get() = fontIcon(MaterialDesign.MDI_GAMEPAD_VARIANT)
    val chart get() = fontIcon(MaterialDesign.MDI_CHART_BAR, color = Color.DARKBLUE)
    val book get() = fontIcon(MaterialDesign.MDI_BOOK_OPEN_OUTLINE, color = Color.BROWN)
    val hdd get() = fontIcon(MaterialDesign.MDI_HARDDISK, color = Colors.green)
    val disc get() = fontIcon(MaterialDesign.MDI_DISC)
    val settings get() = fontIcon(MaterialDesign.MDI_SETTINGS, color = Color.GRAY)
    val wrench get() = fontIcon(MaterialDesign.MDI_WRENCH, color = Color.GRAY)
    val display get() = fontIcon(MaterialDesign.MDI_MONITOR_DASHBOARD, color = Color.DARKBLUE)
    val grid get() = fontIcon(MaterialDesign.MDI_GRID)
    val quit get() = fontIcon(MaterialDesign.MDI_LOGOUT)

    val accept get() = fontIcon(MaterialDesign.MDI_CHECK_CIRCLE, color = Colors.green)
    val cancel get() = fontIcon(MaterialDesign.MDI_CLOSE_CIRCLE, color = Colors.red)
    val close get() = fontIcon(MaterialDesign.MDI_CLOSE, color = Colors.red)
    val closeCircle get() = fontIcon(MaterialDesign.MDI_CLOSE_CIRCLE, color = Colors.red)
    val stop get() = fontIcon(MaterialDesign.MDI_STOP, color = Colors.red)
    val redo get() = fontIcon(MaterialDesign.MDI_REDO, color = Colors.orange)
    val clear get() = fontIcon(MaterialDesign.MDI_CLOSE_CIRCLE_OUTLINE)
    val resetToDefault get() = fontIcon(MaterialDesign.MDI_RESTORE)
    val dots get() = fontIcon(MaterialDesign.MDI_DOTS_VERTICAL)

    val check get() = fontIcon(MaterialDesign.MDI_CHECK, color = Colors.green)
    val valid get() = fontIcon(MaterialDesign.MDI_SHIELD_CHECK, color = Colors.green)
    val invalid get() = fontIcon(MaterialDesign.MDI_SHIELD_OFF, color = Colors.red)
    val unverified get() = fontIcon(MaterialDesign.MDI_SHIELD_OUTLINE, color = Colors.orange)
    val excluded get() = fontIcon(MaterialDesign.MDI_CLOSE_OUTLINE, color = Colors.red)
    val checked get() = fontIcon(MaterialDesign.MDI_CHECKBOX_MARKED_OUTLINE, color = Colors.green)
    val checkX get() = fontIcon(MaterialDesign.MDI_CLOSE_BOX_OUTLINE, color = Colors.red)

    val add get() = fontIcon(MaterialDesign.MDI_PLUS_CIRCLE, color = Colors.green)
    val plus get() = fontIcon(MaterialDesign.MDI_PLUS, color = Colors.green)
    val minus get() = fontIcon(MaterialDesign.MDI_MINUS, color = Colors.red)
    val expand get() = fontIcon(MaterialDesign.MDI_ARROW_EXPAND_VERTICAL)
    val collapse get() = fontIcon(MaterialDesign.MDI_ARROW_COLLAPSE_VERTICAL)
    val unfoldMore get() = fontIcon(MaterialDesign.MDI_UNFOLD_MORE_HORIZONTAL)
    val unfoldLess get() = fontIcon(MaterialDesign.MDI_UNFOLD_LESS_HORIZONTAL)
    val arrowLeft get() = fontIcon(MaterialDesign.MDI_ARROW_LEFT)
    val arrowLeftBold get() = fontIcon(MaterialDesign.MDI_ARROW_LEFT_BOLD)
    val arrowLeftBoldBox get() = fontIcon(MaterialDesign.MDI_ARROW_LEFT_BOLD_BOX)
    val arrowRight get() = fontIcon(MaterialDesign.MDI_ARROW_RIGHT)
    val arrowRightBold get() = fontIcon(MaterialDesign.MDI_ARROW_RIGHT_BOLD)
    val arrowRightBoldBox get() = fontIcon(MaterialDesign.MDI_ARROW_RIGHT_BOLD_BOX)
    val arrowRightCircle get() = fontIcon(MaterialDesign.MDI_ARROW_RIGHT_CIRCLE_OUTLINE)
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
    val edit get() = fontIcon(MaterialDesign.MDI_PENCIL, color = Colors.orange)
    val delete get() = fontIcon(MaterialDesign.MDI_DELETE_FOREVER, color = Colors.red)
    val deleteSolid get() = fontIcon(MaterialDesign.MDI_DELETE, color = Colors.red)

    val tag get() = fontIcon(MaterialDesign.MDI_TAG, color = Color.BLUEVIOLET)
    val search get() = fontIcon(MaterialDesign.MDI_MAGNIFY)
    val sync get() = fontIcon(MaterialDesign.MDI_SYNC, color = Colors.green)

    val text get() = fontIcon(MaterialDesign.MDI_FORMAT_COLOR_TEXT)
    val enterText get() = fontIcon(MaterialDesign.MDI_RENAME_BOX, color = Colors.orange)
    val textbox get() = fontIcon(MaterialDesign.MDI_TEXTBOX)
    val documents get() = fontIcon(MaterialDesign.MDI_BOOK_MULTIPLE_VARIANT)
    val script get() = fontIcon(MaterialDesign.MDI_SCRIPT_TEXT_OUTLINE)
    val date get() = fontIcon(MaterialDesign.MDI_CALENDAR)
    val createDate get() = fontIcon(MaterialDesign.MDI_CALENDAR_PLUS)
    val updateDate get() = fontIcon(MaterialDesign.MDI_CALENDAR_EDIT)
    val thumbnail get() = fontIcon(MaterialDesign.MDI_FILE_IMAGE)
    val poster get() = fontIcon(MaterialDesign.MDI_IMAGE)
    val screenshots get() = fontIcon(MaterialDesign.MDI_IMAGE_MULTIPLE)
    val duplicate get() = fontIcon(MaterialDesign.MDI_CONTENT_DUPLICATE)
    val copy get() = fontIcon(MaterialDesign.MDI_CONTENT_COPY)
    val diff get() = fontIcon(MaterialDesign.MDI_VECTOR_DIFFERENCE)
    val select get() = fontIcon(MaterialDesign.MDI_SELECT)
    val regex get() = fontIcon(MaterialDesign.MDI_REGEX)

    val folder get() = fontIcon(MaterialDesign.MDI_FOLDER_OUTLINE)
    val folders get() = fontIcon(MaterialDesign.MDI_FOLDER_MULTIPLE_OUTLINE, color = Colors.green)
    val folderFilled get() = fontIcon(MaterialDesign.MDI_FOLDER, color = Colors.orange)
    val folderEdit get() = fontIcon(MaterialDesign.MDI_FOLDER_EDIT, color = Colors.orange)
    val folderRemove get() = fontIcon(MaterialDesign.MDI_FOLDER_REMOVE_OUTLINE, color = Colors.red)
    val folderOpen get() = fontIcon(MaterialDesign.MDI_FOLDER_OPEN)
    val folderOpenFilled get() = fontIcon(MaterialDesign.MDI_FOLDER_OPEN, color = Colors.orange)
    val folderSync get() = fontIcon(MaterialDesign.MDI_FOLDER_SYNC_OUTLINE, color = Colors.green)
    val folderSearch get() = fontIcon(MaterialDesign.MDI_FOLDER_SEARCH_OUTLINE)
    val fileTree get() = fontIcon(MaterialDesign.MDI_FILE_TREE)
    val fileQuestion get() = fontIcon(MaterialDesign.MDI_FILE_QUESTION)
    val file get() = fontIcon(MaterialDesign.MDI_FILE_OUTLINE)
    val fileDocument get() = fontIcon(MaterialDesign.MDI_FILE_DOCUMENT_OUTLINE)
    val fileAlert get() = fontIcon(MaterialDesign.MDI_FILE_ALERT_OUTLINE)
    val fileMusic get() = fontIcon(MaterialDesign.MDI_FILE_MUSIC)   // FIXME: Use outline version
    val fileVideo get() = fontIcon(MaterialDesign.MDI_FILE_VIDEO)   // FIXME: Use outline version
    val archive get() = fontIcon(MaterialDesign.MDI_ARCHIVE)
    val clipboard get() = fontIcon(MaterialDesign.MDI_CLIPBOARD_OUTLINE)

    val download get() = fontIcon(MaterialDesign.MDI_CLOUD_DOWNLOAD)
    val upload get() = fontIcon(MaterialDesign.MDI_CLOUD_UPLOAD)
    val export get() = fontIcon(MaterialDesign.MDI_EXPORT)
    val import get() = fontIcon(MaterialDesign.MDI_IMPORT)
    val database get() = fontIcon(MaterialDesign.MDI_DATABASE)
    val addNetwork get() = fontIcon(MaterialDesign.MDI_PLUS_NETWORK)
    val siteMap get() = fontIcon(MaterialDesign.MDI_SITEMAP)
    val lan get() = fontIcon(MaterialDesign.MDI_LAN)
    val link get() = fontIcon(MaterialDesign.MDI_LINK)
    val web get() = fontIcon(MaterialDesign.MDI_WEB)
    val tournament get() = fontIcon(MaterialDesign.MDI_TOURNAMENT)
    val history get() = fontIcon(MaterialDesign.MDI_HISTORY)
    val clockStart get() = fontIcon(MaterialDesign.MDI_CLOCK_START)
    val clockEnd get() = fontIcon(MaterialDesign.MDI_CLOCK_END)
    val databaseCleanup get() = fontIcon(MaterialDesign.MDI_DATABASE_REMOVE)
    val earth get() = fontIcon(MaterialDesign.MDI_EARTH)
    val earthOff get() = fontIcon(MaterialDesign.MDI_EARTH_OFF)
    val masks get() = fontIcon(MaterialDesign.MDI_DRAMA_MASKS)

    val computer get() = fontIcon(MaterialDesign.MDI_DESKTOP_CLASSIC)
    val windows get() = fontIcon(MaterialDesign.MDI_WINDOWS, Color.CORNFLOWERBLUE)
    val linux get() = fontIcon(MaterialDesign.MDI_UBUNTU, Color.ORANGERED)
    val android get() = fontIcon(MaterialDesign.MDI_ANDROID, Color.FORESTGREEN)
    val apple get() = fontIcon(MaterialDesign.MDI_APPLE, Color.GRAY)
    val youTube get() = fontIcon(MaterialDesign.MDI_YOUTUBE, Color.RED)
    val github get() = fontIcon(MaterialDesign.MDI_GITHUB_CIRCLE)
    val gitlab get() = fontIcon(MaterialDesign.MDI_GITLAB)

    val information get() = fontIcon(MaterialDesign.MDI_INFORMATION, color = Color.CADETBLUE)
    val question get() = fontIcon(MaterialDesign.MDI_HELP_CIRCLE_OUTLINE, color = Color.YELLOWGREEN)
    val warning get() = fontIcon(MaterialDesign.MDI_ALERT_OUTLINE, color = Colors.orange)
    val error get() = fontIcon(MaterialDesign.MDI_CLOSE_BOX_OUTLINE, color = Color.INDIANRED)
    val exclamation get() = fontIcon(MaterialDesign.MDI_EXCLAMATION, color = Colors.red)
    val or get() = fontIcon(MaterialDesign.MDI_GATE_OR)
    val and get() = fontIcon(MaterialDesign.MDI_GATE_AND)
    val validationError get() = warning.color(Color.RED)
    val copyright get() = fontIcon(MaterialDesign.MDI_COPYRIGHT)

    val equal get() = fontIcon(MaterialDesign.MDI_EQUAL)
    val notEqual get() = fontIcon(MaterialDesign.MDI_NOT_EQUAL_VARIANT)
    val match get() = fontIcon(MaterialDesign.MDI_TILDE)
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

    val logTrace get() = fontIcon(MaterialDesign.MDI_ALPHA_T_BOX_OUTLINE, color = Color.GRAY)
    val logDebug get() = fontIcon(MaterialDesign.MDI_ALPHA_D_BOX_OUTLINE, color = Colors.niceBlue.darker())
    val logInfo get() = fontIcon(MaterialDesign.MDI_ALPHA_I_BOX_OUTLINE)
    val logWarn get() = fontIcon(MaterialDesign.MDI_ALPHA_W_BOX_OUTLINE, color = Colors.orange)
    val logError get() = fontIcon(MaterialDesign.MDI_ALPHA_E_BOX_OUTLINE, color = Colors.red)
}

fun fontIcon(icon: Ikon, color: Paint = Color.BLACK): FontIcon = FontIcon.of(icon, 30).color(color)
fun FontIcon.color(color: Paint): FontIcon = apply { iconColor = color }
fun FontIcon.size(size: Int): FontIcon = apply { iconSize = size }