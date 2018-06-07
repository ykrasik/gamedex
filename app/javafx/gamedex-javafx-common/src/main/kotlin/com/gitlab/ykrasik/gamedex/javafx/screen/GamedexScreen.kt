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

package com.gitlab.ykrasik.gamedex.javafx.screen

import javafx.scene.control.ToolBar
import org.controlsfx.glyphfont.Glyph

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 15:50
 */
abstract class PresentableScreen(title: String = "", icon: Glyph? = null) : PresentableView(title, icon) {
    abstract fun ToolBar.constructToolbar()

    init {
        // All tabs (which we use as screens) will have 'onDock' called even though they're not actually showing.
        // This is just how TornadoFx works.
        skipFirstDock = true
    }
}