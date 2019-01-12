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

package com.gitlab.ykrasik.gamedex.javafx

import javafx.geometry.Pos
import javafx.scene.paint.Color
import org.controlsfx.control.Notifications
import tornadofx.UIComponent
import tornadofx.seconds

/**
 * User: ykrasik
 * Date: 21/11/2018
 * Time: 08:46
 */
fun UIComponent.notification(text: String): Notifications =
    Notifications.create()
        .owner(currentStage!!)
        .text(text)
        .darkStyle()
        .hideAfter(4.seconds)
        .hideCloseButton()
        .position(Pos.BOTTOM_RIGHT)

val Notifications.info get() = graphic(Icons.information.size(50).color(Color.WHITE))
val Notifications.warn get() = graphic(Icons.warning.size(50))
val Notifications.error get() = graphic(Icons.error.size(50))