/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.javafx.game.delete

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.details.DeleteGameChoice
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialog
import com.gitlab.ykrasik.gamedex.javafx.jfxCheckBox
import javafx.beans.property.SimpleBooleanProperty

/**
 * User: ykrasik
 * Date: 01/05/2018
 * Time: 15:10
 */
fun confirmGameDelete(game: Game): DeleteGameChoice {
    val fromFileSystem = SimpleBooleanProperty(false)
    val confirm = areYouSureDialog("Delete game '${game.name}'?") {
        jfxCheckBox(fromFileSystem, "From File System")
    }
    return if (confirm) {
        DeleteGameChoice.Confirm(fromFileSystem.value)
    } else {
        DeleteGameChoice.Cancel
    }
}