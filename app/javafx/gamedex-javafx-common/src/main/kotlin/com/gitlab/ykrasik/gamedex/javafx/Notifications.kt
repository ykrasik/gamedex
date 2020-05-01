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

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.javafx.control.defaultHbox
import com.gitlab.ykrasik.gamedex.javafx.control.defaultVbox
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.header
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import javafx.geometry.Pos
import org.controlsfx.control.Notifications
import tornadofx.*

/**
 * User: ykrasik
 * Date: 21/11/2018
 * Time: 08:46
 */
fun UIComponent.notification(type: NotificationType, text: String, title: String?) =
    Notifications.create()
        .owner(currentStage!!)
        .hideAfter(5.seconds)
        .hideCloseButton()
        .position(Pos.BOTTOM_RIGHT)
        .graphic(defaultHbox {
            useMaxSize = true
            paddingAll = 20.0
            children += when (type) {
                NotificationType.Info -> Icons.information
                NotificationType.Warn -> Icons.warning
                NotificationType.Error -> Icons.error
            }.size(50)
            defaultVbox(/*alignment = Pos.CENTER*/) {
//                useMaxSize = true
//                paddingAll = 20.0
                if (title != null) {
                    header(title)
                }
                label(text)
            }
        })
        .show()

enum class NotificationType {
    Info, Warn, Error
}