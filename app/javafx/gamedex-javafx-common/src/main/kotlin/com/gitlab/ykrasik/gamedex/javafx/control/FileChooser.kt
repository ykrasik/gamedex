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

package com.gitlab.ykrasik.gamedex.javafx.control

import javafx.stage.FileChooser
import javafx.stage.Window
import tornadofx.FileChooserMode
import java.io.File

/**
 * User: ykrasik
 * Date: 02/05/2020
 * Time: 14:03
 */
// Copied from TornadoFX, with the addition of initialDirectory
fun chooseFile(
    title: String? = null,
    filters: List<FileChooser.ExtensionFilter>,
    mode: FileChooserMode = FileChooserMode.Single,
    initialDirectory: File? = null,
    owner: Window? = null,
    op: FileChooser.() -> Unit = {},
): List<File> {
    val chooser = FileChooser()
    if (title != null) chooser.title = title
    chooser.extensionFilters.addAll(filters)
    chooser.initialDirectory = initialDirectory
    op(chooser)
    return when (mode) {
        FileChooserMode.Single -> {
            val result = chooser.showOpenDialog(owner)
            if (result == null) emptyList() else listOf(result)
        }
        FileChooserMode.Multi -> chooser.showOpenMultipleDialog(owner) ?: emptyList()
        FileChooserMode.Save -> {
            val result = chooser.showSaveDialog(owner)
            if (result == null) emptyList() else listOf(result)
        }
        else -> emptyList()
    }
}