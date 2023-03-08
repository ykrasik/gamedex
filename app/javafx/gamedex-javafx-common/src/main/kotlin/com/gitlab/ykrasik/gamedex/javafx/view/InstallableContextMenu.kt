/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

import com.gitlab.ykrasik.gamedex.javafx.control.determineArrowLocation
import com.gitlab.ykrasik.gamedex.javafx.control.popOver
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * User: ykrasik
 * Date: 07/10/2018
 * Time: 10:29
 */
abstract class InstallableContextMenu<T : Any>(initialData: T) : PresentableView() {
    protected val data = MutableStateFlow(initialData)

    private val popover by lazy {
        popOver { children += root }.apply { isAutoFix = false }
    }

    private val handler = EventHandler<MouseEvent> {
//            if (contextMenuRequested) {
// On mac, the 'setOnContextMenuRequested' click propagates to this event handler.
//                contextMenuRequested = false
//            } else {
        popover.hide()
//            }
    }

    fun install(node: Node, data: () -> T) {
//        var contextMenuRequested = false
        node.addEventHandler(MouseEvent.MOUSE_CLICKED, handler)
        node.setOnContextMenuRequested { e ->
//            contextMenuRequested = true
            this.data.value = data()
            popover.determineArrowLocation(e.screenX, e.screenY).show(node, e.screenX, e.screenY)
        }
    }

    fun uninstall(node: Node) {
        node.onContextMenuRequested = null
        node.removeEventHandler(MouseEvent.MOUSE_CLICKED, handler)
    }
}
