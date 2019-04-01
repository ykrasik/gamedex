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

package com.gitlab.ykrasik.gamedex.app.javafx.common

import com.gitlab.ykrasik.gamedex.app.api.common.AboutView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.control.customToolbar
import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.control.gap
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.acceptButton
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.gitlab.ykrasik.gamedex.javafx.theme.subHeader
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableWindow
import com.gitlab.ykrasik.gamedex.util.defaultTimeZone
import com.gitlab.ykrasik.gamedex.util.humanReadable
import javafx.geometry.Pos
import tornadofx.*

/**
 * User: ykrasik
 * Date: 09/02/2019
 * Time: 17:30
 */
class JavaFxAboutView : PresentableWindow("About"), AboutView {
    private val commonOps: JavaFxCommonOps by di()

    override val acceptActions = channel<Unit>()

    override val root = borderpane {
        top = customToolbar {
            gap(size = 10)
            children += Icons.information.size(30)
            spacer()
            acceptButton {
                isCancelButton = true
                action(acceptActions)
            }
        }
        center = vbox(spacing = 5) {
            paddingAll = 15
            gridpane {
                hgap = 10.0
                vgap = 3.0
                alignment = Pos.CENTER_LEFT
                row {
                    subHeader("Version")
                    label(commonOps.applicationVersion.version)
                }
                row {
                    subHeader("Built On")
                    label(commonOps.applicationVersion.buildDate!!.defaultTimeZone.humanReadable)
                }
                row {
                    subHeader("Commit")
                    label(commonOps.applicationVersion.commitHash!!)
                }
            }
            defaultHbox {
                paddingTop = 40
                spacer()
                label("Copyright 2016-2019 Yevgeny Krasik", graphic = Icons.copyright.size(20))
            }
        }
    }

    init {
        register()
    }
}