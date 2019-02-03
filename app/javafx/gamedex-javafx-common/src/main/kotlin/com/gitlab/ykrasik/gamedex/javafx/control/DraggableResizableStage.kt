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

package com.gitlab.ykrasik.gamedex.javafx.control

import javafx.scene.Cursor
import javafx.scene.input.MouseEvent
import javafx.stage.Stage

/**
 * A modified version of the one created by Alexander Berg [https://stackoverflow.com/a/24017605]
 * that also supports dragging.
 */
fun Stage.draggableResizable(border: Int = 4) {
    var resizeStartX = 0.0
    var resizeStartY = 0.0
    var aboutToResize = false
    var resizing = false

    var dragOffsetX = 0.0
    var dragOffsetY = 0.0
    var dragging = false

    scene.setOnMouseMoved { e ->
        if (scene == null) return@setOnMouseMoved
        scene.cursor = when {
            e.sceneX < border && e.sceneY < border -> Cursor.NW_RESIZE
            e.sceneX < border && e.sceneY > scene.height - border -> Cursor.SW_RESIZE
            e.sceneX > scene.width - border && e.sceneY < border -> Cursor.NE_RESIZE
            e.sceneX > scene.width - border && e.sceneY > scene.height - border -> Cursor.SE_RESIZE
            e.sceneX < border -> Cursor.W_RESIZE
            e.sceneX > scene.width - border -> Cursor.E_RESIZE
            e.sceneY < border -> Cursor.N_RESIZE
            e.sceneY > scene.height - border -> Cursor.S_RESIZE
            else -> Cursor.DEFAULT
        }
        aboutToResize = scene.cursor != Cursor.DEFAULT
    }

    scene.setOnMousePressed { e ->
        if (scene == null) return@setOnMousePressed
        if (aboutToResize) {
            resizing = true
            resizeStartX = width - e.sceneX
            resizeStartY = height - e.sceneY
        } else {
            dragging = true
            dragOffsetX = x - e.screenX
            dragOffsetY = y - e.screenY
            scene.cursor = Cursor.CLOSED_HAND
        }
    }

    scene.addEventFilter(MouseEvent.MOUSE_DRAGGED) { e ->
        if (scene == null) return@addEventFilter
        if (resizing) {
            if (scene.cursor != Cursor.W_RESIZE && scene.cursor != Cursor.E_RESIZE) {
                val minHeight = if (minHeight > border * 2) minHeight else border.toDouble() * 2
                when (scene.cursor) {
                    Cursor.NW_RESIZE, Cursor.N_RESIZE, Cursor.NE_RESIZE -> if (height > minHeight || e.sceneY < 0) {
                        allowedHeight += y - e.screenY
                        y = e.screenY
                    }
                    else -> if (height > minHeight || e.sceneY + resizeStartY - height > 0) {
                        allowedHeight = e.sceneY + resizeStartY
                    }
                }
            }

            if (scene.cursor != Cursor.N_RESIZE && scene.cursor != Cursor.S_RESIZE) {
                val minWidth = if (minWidth > border * 2) minWidth else border.toDouble() * 2
                when (scene.cursor) {
                    Cursor.NW_RESIZE, Cursor.W_RESIZE, Cursor.SW_RESIZE -> if (width > minWidth || e.sceneX < 0) {
                        allowedWidth += x - e.screenX
                        x = e.screenX
                    }
                    else -> if (width > minWidth || e.sceneX + resizeStartX - width > 0) {
                        allowedWidth = e.sceneX + resizeStartX
                    }
                }
            }
            e.consume()
        } else if (dragging) {
            x = e.screenX + dragOffsetX
            y = e.screenY + dragOffsetY
            e.consume()
        }
    }

    scene.setOnMouseReleased {
        if (scene == null) return@setOnMouseReleased
        dragging = false
        resizing = false
        if (!aboutToResize) {
            scene.cursor = Cursor.DEFAULT
        }
    }
}

var Stage.allowedWidth
    get() = width
    set(value) {
        width = Math.max(minWidth, Math.min(value, maxWidth))
    }

var Stage.allowedHeight
    get() = height
    set(value) {
        height = Math.max(minHeight, Math.min(value, maxHeight))
    }