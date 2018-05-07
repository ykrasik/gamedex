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

package com.gitlab.ykrasik.gamedex.javafx.game.tag

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGameChoice
import com.gitlab.ykrasik.gamedex.app.api.game.tag.TagGameView
import com.gitlab.ykrasik.gamedex.app.api.presenters
import com.gitlab.ykrasik.gamedex.javafx.*
import com.gitlab.ykrasik.gamedex.javafx.screen.PresentableView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.SetChangeListener
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

    private val toggleAllProperty = SimpleBooleanProperty(false)
    override var toggleAll by toggleAllProperty

    private val viewModel = TagViewModel()
    override var newTagName by viewModel.nameProperty

    private val nameValidationErrorProperty = SimpleStringProperty(null)
    override var nameValidationError by nameValidationErrorProperty

    private val presenter = presenters.tagGameView.present(this)

    private var choice: TagGameChoice = TagGameChoice.Cancel

    init {
        checkedTags.addListener(SetChangeListener<String> { tags.invalidate() })
        nameValidationErrorProperty.onChange { viewModel.validate() }
    }

    override val root = borderpane {
        addClass(Style.tagWindow)
        top {
            toolbar {
                acceptButton { presentOnAction { presenter.onAccept() } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { presentOnAction { presenter.onCancel() } }
            }
        }
        center {
            vbox(spacing = 10) {
                addClass(Style.tagContent)
                gridpane {
                    hgap = 5.0
                    row {
                        toggleAllButton()
                        verticalSeparator()
                        addTagButton()
                    }
                }
                separator()
                existingTags()
            }
        }
    }

    private fun EventTarget.toggleAllButton() = jfxToggleButton(toggleAllProperty, "Toggle All") {
        tooltip("Toggle all")
        selectedProperty().presentOnChange { presenter.onToggleAllChanged(it) }
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
            presentOnAction { presenter.onAddNewTag() }
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
                        selectedProperty().presentOnChange { presenter.onTagToggleChanged(tag, it) }
                    }
                }
            }
        }
    }

    fun show(game: Game): TagGameChoice {
        presenter.onShown(game)
        openWindow(block = true)
        return choice
    }

    override fun close(choice: TagGameChoice) {
        this.choice = choice
        close()
    }

    private inner class TagViewModel : ViewModel() {
        val nameProperty = presentableProperty({ presenter.onNewTagNameChanged(it) }, { SimpleStringProperty("") })
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
