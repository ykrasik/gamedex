package com.gitlab.ykrasik.gamedex.ui.view.game.rename

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import javafx.beans.property.Property
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.stage.StageStyle
import tornadofx.*
import java.io.File
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 19:47
 */
class RenameFolderFragment(private val game: Game, initialSuggestion: String) : Fragment("Rename/Move '${game.path}'") {
    private var accept = false

    private val newPathProperty = initialSuggestion.toProperty()
    private val model = RenameFolderViewModel(newPathProperty)

    private val basePathProperty = game.path.parentFile.toProperty().apply {
        onChange { model.commit() }
    }
    private var basePath by basePathProperty

    override val root = borderpane {
        minWidth = 700.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton {
                    enableWhen { model.valid }
                    setOnAction { close(accept = true) }
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    setOnAction { close(accept = false) }
                }
            }
        }
        center {
            form {
                paddingAll = 20.0
                fieldset {
                    field("From") { pathButton(game.path) }
                    field("To") {
                        hbox(spacing = 1.0) {
                            alignment = Pos.CENTER_LEFT
                            jfxButton {
                                textProperty().bind(basePathProperty.map { it!!.toString() })
                                setOnAction {
                                    basePath = chooseDirectory("Browse Game Path...", initialDirectory = basePath) ?: return@setOnAction
                                }
                            }
                            textfield(model.pathProperty) {
                                hgrow = Priority.ALWAYS
                                validator {
                                    val valid = try {
                                        it!!.isValidFile()
                                    } catch (e: Exception) {
                                        false
                                    }
                                    if (!valid) error("Invalid folder name!") else null
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun String.isValidFile() = File(basePath, this).let { new ->
        !contains(File.separatorChar) && new.isValid() && new.doesntExist() //&& new.parentFile.exists()
    }

    private fun File.isValid() = try {
        Paths.get(this.toURI())
        true
    } catch (e: Exception) {
        false
    }

    private fun File.doesntExist(): Boolean {
        if (!this.exists()) return true

        // Windows is case insensitive.
        return this.path != game.path.path && this.path.equals(game.path.path, ignoreCase = true)
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): File? {
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
        return if (accept && model.commit()) {
            File(basePath, model.pathProperty.value)
        } else {
            null
        }
    }

    private class RenameFolderViewModel(p: Property<String>) : ViewModel() {
        val pathProperty = bind { p }

        init {
            pathProperty.onChange { commit() }
            validate(decorateErrors = true)
        }
    }
}