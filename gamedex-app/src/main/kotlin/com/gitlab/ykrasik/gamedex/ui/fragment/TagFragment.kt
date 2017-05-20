package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.controller.GameController
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.collections.FXCollections
import javafx.geometry.Orientation
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 10:12
 */
class TagFragment(game: Game) : Fragment("Tag") {
    private val gameController: GameController by di()

    private val tags = FXCollections.observableArrayList(gameController.tags)
    private val checkedTags = HashSet(game.tags).observable()

    private var choice: Choice = Choice.Cancel

    override val root = borderpane {
        addClass(Style.tagWindow)
        top {
            toolbar {
                acceptButton { setOnAction { close(choice = Choice.Select(checkedTags.toList())) } }
                verticalSeparator()
                spacer()
                verticalSeparator()
                cancelButton { setOnAction { close(choice = Choice.Cancel) } }
            }
        }
        center {
            vbox(spacing = 10) {
                addClass(Style.tagContent)
                gridpane {
                    hgap = 5.0
                    row {
                        jfxToggleButton {
                            tooltip("Toggle all")
                            isSelected = tags.toSet() == checkedTags
                            selectedProperty().onChange {
                                if (it) checkedTags.addAll(tags) else checkedTags.clear()
                                tags.invalidate()
                            }
                        }

                        verticalSeparator()

                        label("New Tag:")
                        val newTagName = textfield {
                            setId(Style.newTagTextField)
                            promptText = "Tag Name"
                            isFocusTraversable = false
                        }
                        val alreadyExists = tags.containing(newTagName.textProperty())
                        jfxButton(graphic = FontAwesome.Glyph.PLUS.toGraphic()) {
                            disableWhen {
                                newTagName.textProperty().let { name ->
                                    name.isEmpty.or(name.isNull).or(alreadyExists)
                                }
                            }
                            defaultButtonProperty().bind(newTagName.focusedProperty())
                            setOnAction {
                                checkTag(newTagName.text)
                                tags += newTagName.text
                                newTagName.clear()
                            }
                        }
                    }
                }
                separator()
                flowpane {
                    addClass(Style.tagDisplay)
                    tags.performing { tags ->
                        replaceChildren {
                            tags.sorted().forEach { tag ->
                                jfxToggleButton {
                                    text = tag
                                    isSelected = checkedTags.contains(tag)
                                    selectedProperty().onChange {
                                        if (it) checkTag(tag) else uncheckTag(tag)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkTag(tag: String) { checkedTags += tag }
    private fun uncheckTag(tag: String) { checkedTags -= tag }

    fun show(): Choice {
        openWindow(block = true, owner = null)
        return choice
    }

    private fun close(choice: Choice) {
        this.choice = choice
        close()
    }

    sealed class Choice {
        data class Select(val tags: List<String>) : Choice()
        object Cancel : Choice()
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
