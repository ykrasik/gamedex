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

package com.gitlab.ykrasik.gamedex.javafx.view

import com.gitlab.ykrasik.gamedex.app.api.util.MultiChannel
import com.gitlab.ykrasik.gamedex.javafx.control.determineArrowLocation
import com.gitlab.ykrasik.gamedex.javafx.control.popOver
import javafx.scene.Node
import javafx.scene.input.MouseEvent

/**
 * User: ykrasik
 * Date: 07/10/2018
 * Time: 10:29
 */
abstract class InstallableContextMenu<T : Any> : PresentableView() {
    protected lateinit var data: T
    protected val dataChannel = MultiChannel.conflated<T>()

    private val popover by lazy {
        popOver { children += root }.apply { isAutoFix = false }
    }

    fun install(node: Node, data: () -> T) {
//        var contextMenuRequested = false
        node.addEventHandler(MouseEvent.MOUSE_CLICKED) {
//            if (contextMenuRequested) {
                // On mac, the 'setOnContextMenuRequested' click propagates to this event handler.
//                contextMenuRequested = false
//            } else {
                popover.hide()
//            }
        }
        node.setOnContextMenuRequested { e ->
//            contextMenuRequested = true
            this.data = data()
            dataChannel.offer(this.data)
            popover.determineArrowLocation(e.screenX, e.screenY).show(node, e.screenX, e.screenY)
        }
    }
}