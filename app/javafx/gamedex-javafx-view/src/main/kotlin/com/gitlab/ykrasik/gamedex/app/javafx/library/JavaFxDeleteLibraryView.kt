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

package com.gitlab.ykrasik.gamedex.app.javafx.library

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.app.api.library.DeleteLibraryView
import com.gitlab.ykrasik.gamedex.javafx.control.fitAtMost
import com.gitlab.ykrasik.gamedex.javafx.perform
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/05/2018
 * Time: 10:03
 */
class JavaFxDeleteLibraryView : ConfirmationWindow(icon = Icons.delete), DeleteLibraryView {
    private val libraryProperty = SimpleObjectProperty(Library.Null)
    override var library: Library by libraryProperty

    override val gamesToBeDeleted = mutableListOf<Game>().asObservable()

    init {
        titleProperty.bind(libraryProperty.stringBinding { "Delete library '${it!!.name}'?" })
        register()
    }

    override val root = buildAreYouSure {
        gamesToBeDeleted.perform { gamesToBeDeleted ->
            this.replaceChildren {
                if (gamesToBeDeleted.isNotEmpty()) {
                    label("The following ${gamesToBeDeleted.size} games will also be deleted:")
                    listview(gamesToBeDeleted.map { it.name }.asObservable()) { fitAtMost(10) }
                }
            }
        }
    }
}