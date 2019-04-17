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

package com.gitlab.ykrasik.gamedex.app.javafx.game.details

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.javafx.control.NotBuggedJFXTreeCell
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTreeView
import com.gitlab.ykrasik.gamedex.javafx.control.keepSelectionInView
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.jfoenix.controls.JFXTreeView
import javafx.event.EventTarget
import javafx.scene.control.TreeItem
import javafx.scene.layout.HBox
import tornadofx.label
import tornadofx.spacer
import tornadofx.treeitem

/**
 * User: ykrasik
 * Date: 17/04/2019
 * Time: 08:15
 */
fun EventTarget.fileTreeView(fileTree: FileTree, op: JFXTreeView<FileTree>.() -> Unit = {}) = jfxTreeView<FileTree> {
    keepSelectionInView()

    setCellFactory {
        object : NotBuggedJFXTreeCell<FileTree>() {
            override fun updateItem(item: FileTree?, empty: Boolean) {
                super.updateItem(item, empty)
                if (item == null) return

                graphic = HBox().apply {
                    val graphic = (
                        if (item.isDirectory)
                            Icons.folderOpenFilled
                        else
                            fileIcons.getValue(item.name.substringAfterLast('.')).get()
                        ).size(20)
                    label(item.name, graphic)
                    spacer()
                    label(item.size.humanReadable)
                }
            }
        }
    }

    root = TreeItem(fileTree).apply { isExpanded = true }

    fileTree.children.sortedBy { !it.isDirectory }.forEach { root.add(it) }

    op()
}

private fun TreeItem<FileTree>.add(fileTree: FileTree) {
    val item = treeitem(fileTree) { isExpanded = true }
    fileTree.children.sortedBy { !it.isDirectory }.forEach { item.add(it) }
}

val fileIcons = run {
    val text = listOf("nfo", "txt", "inf", "ini", "log").map { it to Icons::fileDocument }
    val binary = listOf("iso").map { it to Icons::disc }
    val archive = listOf("zip", "rar").map { it to Icons::archive }
    val executable = listOf("exe", "bat").map { it to Icons::fileAlert }
    val image = listOf("jpg", "png", "bmp", "gif", "ico").map { it to Icons::thumbnail }
    val video = listOf("mp4", "mkv", "avi", "mov").map { it to Icons::fileVideo }
    val music = listOf("mp3", "wav", "ogg").map { it to Icons::fileMusic }

    (text + binary + archive + executable + image + video + music)
        .toMap().withDefault { Icons::file }
}