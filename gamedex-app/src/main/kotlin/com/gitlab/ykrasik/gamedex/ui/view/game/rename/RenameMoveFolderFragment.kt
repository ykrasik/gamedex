package com.gitlab.ykrasik.gamedex.ui.view.game.rename

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.controller.LibraryController
import com.gitlab.ykrasik.gamedex.ui.jfxButton
import com.gitlab.ykrasik.gamedex.ui.map
import com.gitlab.ykrasik.gamedex.ui.popoverComboMenu
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import com.gitlab.ykrasik.gamedex.ui.theme.header
import com.gitlab.ykrasik.gamedex.ui.theme.pathButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import com.gitlab.ykrasik.gamedex.util.toFile
import javafx.beans.property.Property
import javafx.collections.FXCollections
import javafx.geometry.HPos
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
class RenameMoveFolderFragment(private val game: Game, initialSuggestion: String) : Fragment("Rename/Move '${game.path}'") {
    private val libraryController: LibraryController by di()

    private var accept = false

    private val nameProperty = initialSuggestion.toProperty()
    private var name by nameProperty

    private val model = RenameFolderViewModel(nameProperty)

    private val libraryProperty = game.library.toProperty().apply {
        onChange { model.commit() }
    }
    private var library by libraryProperty

    private var pathProperty = game.rawGame.metaData.path.toFile().let { it.parentFile?.path ?: "" }.toProperty().apply {
        onChange { model.commit() }
    }
    private var path by pathProperty

    private val basePath get() = library.path.resolve(path).normalize()

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
                    separator()
                    field("To") {
                        gridpane {
                            header("Library") { gridpaneConstraints { columnRowIndex(0, 0); hAlignment = HPos.CENTER } }
                            popoverComboMenu(
                                // TODO: This is to avoid a listener leak, due to possibleItems being observable
                                possibleItems = FXCollections.observableArrayList(libraryController.allLibraries),
                                selectedItemProperty = libraryProperty,
                                text = { it.path.path }
                            ).apply {
                                gridpaneConstraints { columnRowIndex(0, 1); hAlignment = HPos.LEFT }
                            }

                            verticalSeparator { gridpaneConstraints { columnRowIndex(1, 0); rowSpan = 2 } }

                            header("Path") { gridpaneConstraints { columnRowIndex(2, 0); hAlignment = HPos.CENTER } }
                            jfxButton {
                                gridpaneConstraints { columnRowIndex(2, 1); hAlignment = HPos.LEFT }
                                useMaxWidth = true
                                textProperty().bind(pathProperty.map { if (it!!.isEmpty()) File.separator else it })
                                setOnAction {
                                    val initialDirectory = library.path.resolve(path).let { dir ->
                                        if (dir.exists()) dir else library.path
                                    }
                                    chooseDirectory("Browse Path...", initialDirectory = initialDirectory)?.let { newPath ->
                                        path = newPath.relativeTo(library.path).path
                                    }
                                }
                            }

                            verticalSeparator { gridpaneConstraints { columnRowIndex(3, 0); rowSpan = 2 } }

                            header("Name") { gridpaneConstraints { columnRowIndex(4, 0); hAlignment = HPos.CENTER } }
                            textfield(model.pathProperty) {
                                gridpaneConstraints { columnRowIndex(4, 1); hAlignment = HPos.LEFT; hGrow = Priority.ALWAYS }
                                validator {
                                    val valid = try {
                                        it!!.isValidFile()
                                    } catch (e: Exception) {
                                        false
                                    }
                                    when {
                                        !valid -> error("Invalid folder name!")
                                        !isValidBasePath() -> error("Must be in the same library: ${library.path}")
                                        it!!.alreadyExists() -> error("Already exists!")
                                        else -> null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun String.isValidFile() = basePath.resolve(this).let { new ->
        !contains(File.separatorChar) && try {
            Paths.get(new.toURI())
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun String.alreadyExists(): Boolean {
        val file = basePath.resolve(this)
        if (!file.exists()) return false

        // Windows is case insensitive.
        return file.path == game.path.path || !file.path.equals(game.path.path, ignoreCase = true)
    }

    private fun isValidBasePath(): Boolean =
        basePath.startsWith(library.path) &&
            libraryController.allLibraries.filter { !library.path.startsWith(it.path) }.none { basePath.startsWith(it.path) }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): Pair<Library, String>? {
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
        return if (accept && model.commit()) {
            library to path.toFile().resolve(name).path
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