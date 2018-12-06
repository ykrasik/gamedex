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
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.areYouSureDialogContainer
import com.gitlab.ykrasik.gamedex.javafx.control.fitAtMost
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
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

    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        titleProperty.bind(libraryProperty.stringBinding { "Delete Library '${it?.name}'?"})
        viewRegistry.onCreate(this)
    }

    override val root = areYouSureDialogContainer(acceptActions, cancelActions, titleProperty) {
        gamesToBeDeleted.perform { gamesToBeDeleted ->
            this.replaceChildren {
                if (gamesToBeDeleted.isNotEmpty()) {
                    label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                    listview(gamesToBeDeleted.map { it.name }.observable()) { fitAtMost(10) }
                }
            }
            modalStage?.sizeToScene()
        }
    }
}