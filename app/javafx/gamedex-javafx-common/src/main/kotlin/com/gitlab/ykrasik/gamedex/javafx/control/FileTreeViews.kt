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

package com.gitlab.ykrasik.gamedex.javafx.control

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.size
import com.jfoenix.controls.JFXTreeView
import javafx.event.EventTarget
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*
import java.awt.Desktop
import java.io.File
import java.util.*

/**
 * User: ykrasik
 * Date: 17/04/2019
 * Time: 08:15
 */
fun EventTarget.prettyFileTreeView(
    root: FileTree,
    mainExecutableFileTree: FileTree? = null,
    op: PrettyListView<FileTree>.() -> Unit = {},
) = prettyListView<FileTree> {
    val (files, indents) = processFileTree(root)
    items.setAll(files)
    prefHeight = files.size * 26.0

    prettyListCell { fileTree ->
        paddingAll = 3.0
        text = null
        graphic = HBox().apply {
            gap(size = indents.getValue(fileTree) * 15)

            val graphic = when {
                item.isDirectory -> Icons.folderOpenFilled
                item === mainExecutableFileTree -> Icons.play
                else -> fileIcons.getValue(item.name.substringAfterLast('.')).get()
            }.size(20)
            label(item.name, graphic)

            spacer()

            label(item.size.humanReadable)

            style { padding = box(0.px) }
        }
    }

    op()
}

private fun processFileTree(root: FileTree): Pair<List<FileTree>, IdentityHashMap<FileTree, Int>> {
    val files = mutableListOf<FileTree>()
    val indents = IdentityHashMap<FileTree, Int>()

    fun calc0(fileTree: FileTree, indent: Int) {
        files += fileTree
        indents += fileTree to indent
        fileTree.children.forEach { calc0(it, indent + 1) }
    }
    calc0(root, 0)
    return files to indents
}

fun EventTarget.fileTreeView(fileTree: FileTree, basePath: File, op: JFXTreeView<FileTree>.() -> Unit = {}) = jfxTreeView<FileTree> {
    keepSelectionInView()

    setCellFactory {
        object : NotBuggedJFXTreeCell<FileTree>() {
            override fun updateItem(item: FileTree?, empty: Boolean) {
                super.updateItem(item, empty)
                if (item == null) return

                graphic = HBox().apply {
                    val graphic = (
                        if (item.isDirectory) Icons.folderOpenFilled
                        else fileIcons.getValue(item.name.substringAfterLast('.')).get()
                        ).size(20)
                    label(item.name, graphic)
                    spacer()
                    label(item.size.humanReadable)
                }
            }
        }
    }

    onUserSelect(clickCount = 2) {
        GlobalScope.launch(Dispatchers.IO) {
            val file = basePath.parentFile.resolve(it.determineFile())
            Desktop.getDesktop().open(file)
        }
    }

    root = TreeItem(fileTree.copy(name = basePath.resolve(fileTree.name).toString())).apply { isExpanded = true }

    fileTree.children.sortedBy { !it.isDirectory }.forEach { root.add(it) }

    op()
}

private fun TreeItem<FileTree>.add(fileTree: FileTree) {
    val item = treeitem(fileTree) { isExpanded = true }
    fileTree.children.sortedBy { !it.isDirectory }.forEach { item.add(it) }
}

private fun TreeItem<FileTree>.determineFile(): File =
    if (parent == null) {
        File(value.name)
    } else {
        parent.determineFile().resolve(value.name)
    }

inline fun <T> TreeView<T>.onUserSelect(
    clickCount: Int = 2,
    bindEnter: Boolean = false,
    crossinline action: (TreeItem<T>) -> Unit,
) {
    addEventFilter(MouseEvent.MOUSE_CLICKED) { event ->
        val selectedItem = selectionModel.selectedItem
        if (event.clickCount == clickCount && selectedItem != null) {
            action(selectedItem)
        }
    }

    if (bindEnter) {
        addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            val selectedItem = this.selectionModel.selectedItem
            if (event.code == KeyCode.ENTER && !event.isMetaDown && selectedItem != null) {
                action(selectedItem)
            }
        }
    }
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