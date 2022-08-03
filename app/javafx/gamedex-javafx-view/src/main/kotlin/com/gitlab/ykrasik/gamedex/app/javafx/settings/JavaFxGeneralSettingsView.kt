/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.GeneralSettingsView
import com.gitlab.ykrasik.gamedex.javafx.control.horizontalField
import com.gitlab.ykrasik.gamedex.javafx.control.jfxCheckBox
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTextField
import com.gitlab.ykrasik.gamedex.javafx.control.showWhen
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableTabView
import com.gitlab.ykrasik.gamedex.javafx.viewMutableStateFlow
import tornadofx.fieldset
import tornadofx.form

/**
 * User: ykrasik
 * Date: 15/05/2019
 * Time: 22:22
 */
class JavaFxGeneralSettingsView : PresentableTabView("General", Icons.tune), GeneralSettingsView {
    override val useInternalBrowser = viewMutableStateFlow(true, debugName = "useInternalBrowser")
    override val customBrowserCommand = viewMutableStateFlow("", debugName = "customBrowserCommand")

    override val root = form {
        fieldset {
            horizontalField("Use Internal Browser") { jfxCheckBox(useInternalBrowser.property) }
            horizontalField("Custom External Browser Command") {
                showWhen { useInternalBrowser.property.not() }
                jfxTextField(customBrowserCommand.property, promptText = "For example (Windows): \"cmd /c start chrome --incognito\"") {
                }
            }
        }
    }

    init {
        register()
    }
}
