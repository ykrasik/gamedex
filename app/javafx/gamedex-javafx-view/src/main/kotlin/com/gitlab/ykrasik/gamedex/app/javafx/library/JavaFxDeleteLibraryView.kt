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

package com.gitlab.ykrasik.gamedex.app.javafx.library

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.javafx.dialog.areYouSureDialogContainer
import com.gitlab.ykrasik.gamedex.javafx.fitAtMost
import com.gitlab.ykrasik.gamedex.javafx.performing
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/05/2018
 * Time: 10:03
 */
class JavaFxDeleteLibraryView : PresentableView(), DeleteLibraryView {
    private val libraryProperty = SimpleObjectProperty<Library>()
    override var library: Library by libraryProperty

    override val gamesToBeDeleted = mutableListOf<Game>().observable()

    override val acceptActions = BroadcastEventChannel<Unit>()
    override val cancelActions = BroadcastEventChannel<Unit>()

    init {
        titleProperty.bind(libraryProperty.stringBinding { "Delete Library '${it?.name}'?"})
        viewService.register(this)
    }

    override val root = areYouSureDialogContainer(acceptActions, cancelActions, titleProperty) {
        gamesToBeDeleted.performing { gamesToBeDeleted ->
            this.replaceChildren {
                if (gamesToBeDeleted.isNotEmpty()) {
                    label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                    listview(gamesToBeDeleted.map { it.name }.observable()) { fitAtMost(10) }
                }
            }
            modalStage?.sizeToScene()
        }
    }

    fun show(library: Library) {
        this.library = library
        openModal()
    }

    override fun closeView() = close()
}