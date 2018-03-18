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

package com.gitlab.ykrasik.gamedex.ui.view

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ToolBar
import org.controlsfx.glyphfont.Glyph
import tornadofx.View

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 15:50
 */
abstract class GamedexScreen(title: String, icon: Glyph?) : View(title, icon) {
    abstract fun ToolBar.constructToolbar()

    open val useDefaultNavigationButton: Boolean = true

    // Yuck
    val closeRequestedProperty = SimpleBooleanProperty(false)
}