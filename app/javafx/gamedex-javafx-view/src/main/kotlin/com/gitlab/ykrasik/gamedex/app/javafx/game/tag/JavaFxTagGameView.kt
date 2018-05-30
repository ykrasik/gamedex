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
import com.gitlab.ykrasik.gamedex.app.api.util.BroadcastEventChannel
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
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

    override val checkAllChanges = BroadcastEventChannel<Boolean>()
    private val toggleAllProperty = SimpleBooleanProperty(false).eventOnChange(checkAllChanges)
    override var toggleAll by toggleAllProperty

    override val checkTagChanges = BroadcastEventChannel<Pair<String, Boolean>>()

    override val newTagNameChanges = BroadcastEventChannel<String>()
    private val viewModel = TagViewModel()
    override var newTagName by viewModel.nameProperty

    private inner class TagViewModel : ViewModel() {
        val nameProperty = presentableStringProperty(newTagNameChanges)
    }

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    override val addNewTagActions = BroadcastEventChannel<Unit>()
    override val acceptActions = BroadcastEventChannel<Unit>()
    override val cancelActions = BroadcastEventChannel<Unit>()

    init {
        checkedTags.onChange { tags.invalidate() }
        nameValidationErrorProperty.onChange { viewModel.validate() }
        viewRegistry.register(this)
    }

    override val root = borderpane {
        addClass(Style.tagWindow)
        top {
            toolbar {
                acceptButton { eventOnAction(acceptActions) }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { eventOnAction(cancelActions) }
            }
        }
        center {
            vbox(spacing = 10) {
                addClass(Style.tagContent)
                gridpane {
                    hgap = 5.0
                    row {
                        jfxToggleButton(toggleAllProperty, "Toggle All") {
                            tooltip("Toggle all")
                        }
                        verticalSeparator()
                        addTagButton()
                    }
                }
                separator()
                existingTags()
            }
        }
    }

    private fun EventTarget.addTagButton() {
        label("New Tag:")
        val newTagName = textfield(viewModel.nameProperty) {
            setId(Style.newTagTextField)
            promptText = "Tag Name"
            isFocusTraversable = false
            validator(ValidationTrigger.None) {
                nameValidationError?.let { if (it.isEmpty()) null else error(it) }
            }
        }

        jfxButton(graphic = Theme.Icon.plus(20.0)) {
            disableWhen { nameValidationErrorProperty.isNotNull }
            defaultButtonProperty().bind(newTagName.focusedProperty())
            eventOnAction(addNewTagActions)
        }
    }

    private fun EventTarget.existingTags() = flowpane {
        addClass(Style.tagDisplay)
        tags.performing { tags ->
            replaceChildren {
                tags.sorted().forEach { tag ->
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
            val tagContent by cssclass()
            val newTagTextField by cssid()
            val tagDisplay by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            tagWindow {
                minWidth = 600.px
                minHeight = 600.px
            }

            tagContent {
                padding = box(20.px)
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
