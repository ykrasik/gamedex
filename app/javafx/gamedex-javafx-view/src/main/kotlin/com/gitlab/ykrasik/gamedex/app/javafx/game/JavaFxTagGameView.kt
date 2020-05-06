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

package com.gitlab.ykrasik.gamedex.app.javafx.game

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.TagGameView
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.theme.Icons
import com.gitlab.ykrasik.gamedex.javafx.theme.plusButton
import com.gitlab.ykrasik.gamedex.javafx.view.ConfirmationWindow
import com.gitlab.ykrasik.gamedex.util.IsValid
import javafx.event.EventTarget
import javafx.geometry.Orientation
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:12
 */
class JavaFxTagGameView : ConfirmationWindow("Tag", Icons.tag), TagGameView {
    override val game = viewMutableStatefulChannel(Game.Null)

    override val tags = settableList<String>()
    override val checkedTags = mutableSetOf<String>().observable()

    override val toggleAll = viewMutableStatefulChannel(false)

    override val checkTagChanges = channel<Pair<String, Boolean>>()

    override val newTagName = viewMutableStatefulChannel("")

    override val newTagNameIsValid = statefulChannel(IsValid.valid)

    override val addNewTagActions = channel<Unit>()

    init {
        titleProperty.bind(game.property.stringBinding { "Tag '${it!!.name}'" })
        checkedTags.onChange { tags.invalidate() }
        register()
    }

    override val root = borderpane {
        addClass(Style.tagWindow)
        top = confirmationToolbar()
        center {
            vbox(spacing = 10) {
                paddingAll = 10
                defaultHbox {
                    jfxToggleButton(toggleAll.property, "Toggle All") {
                        tooltip("Toggle all")
                    }
                    addTagButton()
                }
                verticalGap()
                existingTags()
            }
        }
    }

    private fun EventTarget.addTagButton() {
        label("New Tag:")
        val newTagName = jfxTextField(newTagName.property, promptText = "Enter Tag Name...") {
            setId(Style.newTagTextField)
            isFocusTraversable = false
            validWhen(newTagNameIsValid)
        }

        plusButton {
            enableWhen(newTagNameIsValid)
            defaultButtonProperty().bind(newTagName.focusedProperty())
            action(addNewTagActions)
        }
    }

    private fun EventTarget.existingTags() = flowpane {
        addClass(Style.tagDisplay)
        tags.perform { tags ->
            replaceChildren {
                tags.forEach { tag ->
                    jfxToggleButton {
                        text = tag
                        isSelected = checkedTags.contains(tag)
                        selectedProperty().bindChanges(checkTagChanges) { tag to it }
                    }
                }
            }
        }
    }

    class Style : Stylesheet() {
        companion object {
            val tagWindow by cssclass()
            val newTagTextField by cssid()
            val tagDisplay by cssclass()

            init {
                importStylesheetSafe(Style::class)
            }
        }

        init {
            tagWindow {
                minWidth = 600.px
                minHeight = 600.px
            }

            newTagTextField {
                minWidth = 200.px
            }

            tagDisplay {
                prefHeight = 600.px
                orientation = Orientation.VERTICAL
            }
        }
    }
}
