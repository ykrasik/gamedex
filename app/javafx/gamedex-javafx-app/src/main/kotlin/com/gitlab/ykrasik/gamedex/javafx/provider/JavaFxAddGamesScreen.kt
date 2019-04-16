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

package com.gitlab.ykrasik.gamedex.javafx.provider

import com.gitlab.ykrasik.gamedex.app.api.provider.AddGamesView
import com.gitlab.ykrasik.gamedex.app.javafx.provider.JavaFxProviderSearchView
import com.gitlab.ykrasik.gamedex.javafx.addComponent
import com.gitlab.ykrasik.gamedex.javafx.control.jfxTabPane
import com.gitlab.ykrasik.gamedex.javafx.control.popoverComboMenu
import com.gitlab.ykrasik.gamedex.javafx.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableScreen
import javafx.beans.property.SimpleStringProperty
import javafx.scene.layout.HBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 14/04/2019
 * Time: 17:15
 */
class JavaFxAddGamesScreen : PresentableScreen("Add Games", Icons.add), AddGamesView {
    private val providerSearchView: JavaFxProviderSearchView by inject()
    
    private val tabPane = jfxTabPane {
        addClass(CommonStyle.hiddenTabPaneHeader)
        paddingAll = 5
    }

//    private val navigationToggle = ToggleGroup().apply {
//        disallowDeselection()
//        selectedToggleProperty().onChange {
//            tabPane.selectionModel.select(it!!.userData as Tab)
//        }
//    }

    init {
        register()
    }

    override val root = hbox {
        useMaxWidth = true
        addComponent(providerSearchView) {
            root.useMaxWidth = true
            root.hgrow = javafx.scene.layout.Priority.ALWAYS
        }
    }

    override fun HBox.buildToolbar() {
        val selectedItem = SimpleStringProperty()
        popoverComboMenu(listOf("Digital", "Physical"), selectedItem, graphic = { if (it == "Digital") Icons.folder else Icons.disc}) {  }
        selectedItem.onChange {
            val index = if (it == "Digital") 0 else 1
            tabPane.selectionModel.select(index)
        }
//        jfxToggleNode("Digital", Icons.folder, navigationToggle) {  }
//        jfxToggleNode("Physical", Icons.disc, navigationToggle) {  }
    }
}