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

package com.gitlab.ykrasik.gamedex.app.javafx.game.tag

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.TagGameView
import com.gitlab.ykrasik.gamedex.app.api.util.IsValid
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.control.*
import com.gitlab.ykrasik.gamedex.javafx.view.PresentableView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventTarget
import javafx.geometry.Orientation
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:12
 */
class JavaFxTagGameView : PresentableView("Tag"), TagGameView {
    override val tags = mutableListOf<String>().observable()
    override val checkedTags = mutableSetOf<String>().observable()

    private val gameProperty = SimpleObjectProperty<Game>()
    override var game by gameProperty

    override val checkAllChanges = channel<Boolean>()
    private val toggleAllProperty = SimpleBooleanProperty(false).eventOnChange(checkAllChanges)
    override var toggleAll by toggleAllProperty

    override val checkTagChanges = channel<Pair<String, Boolean>>()

    override val newTagNameChanges = channel<String>()
    private val newTagNameProperty = SimpleStringProperty("").eventOnChange(newTagNameChanges)
    override var newTagName by newTagNameProperty

    private val newTagNameIsValidProperty = SimpleObjectProperty(IsValid.valid)
    override var newTagNameIsValid by newTagNameIsValidProperty

    override val addNewTagActions = channel<Unit>()
    override val acceptActions = channel<Unit>()
    override val cancelActions = channel<Unit>()

    init {
        checkedTags.onChange { tags.invalidate() }
        viewRegistry.onCreate(this)
    }

    override val root = borderpane {
        addClass(Style.tagWindow)
        top {
            toolbar {
                cancelButton { eventOnAction(cancelActions) }
                spacer()
                acceptButton { eventOnAction(acceptActions) }
            }
        }
        center {
            vbox(spacing = 10) {
                paddingAll = 10
                defaultHbox {
                    jfxToggleButton(toggleAllProperty, "Toggle All") {
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
        val newTagName = jfxTextField(newTagNameProperty, promptText = "Tag Name") {
            setId(Style.newTagTextField)
            isFocusTraversable = false
            validWhen(newTagNameIsValidProperty)
        }

        plusButton {
            enableWhen(newTagNameIsValidProperty)
            defaultButtonProperty().bind(newTagName.focusedProperty())
            eventOnAction(addNewTagActions)
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
                        selectedProperty().eventOnChange(checkTagChanges) { tag to it }
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
