package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.repository.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.settings.GeneralSettings
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.toFile
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import org.controlsfx.control.PopOver
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class LibraryFragment(existingLibraries: List<Library>, private val library: Library?) :
    Fragment(if (library == null) "Add New Library" else "Edit Library '${library.name}'") {

    private val settings: GeneralSettings by di()

    private val model = SourceViewModel()
    private var accept = false

    override val root = borderpane {
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
                minWidth = 600.0

                fieldset(if (library == null) "Add New Library" else "Edit Library '${library.name}'") {
                    field("Path") {
                        textfield(model.pathProperty) {
                            validator { path ->
                                when {
                                    path.isNullOrBlank() -> error("Path is required!")
                                    !path!!.toFile().isDirectory -> error("Path doesn't exist!")
                                    existingLibraries.any { it != library && it.path == path.toFile() } -> error("Path already in use!")
                                    else -> null
                                }
                            }
                            if (library != null) text = library.path.toString()
                        }
                        jfxButton("Browse", FontAwesome.Glyph.SEARCH.toGraphic()) { setOnAction { browse() } }
                    }
                    field("Name") {
                        textfield(model.nameProperty) {
                            validator { name ->
                                when {
                                    name.isNullOrBlank() -> error("Name is required!")
                                    existingLibraries.any { it != library && it.name == name && it.platform == model.platform } ->
                                        error("Name already in use for this platform!")
                                    else -> null
                                }
                            }
                            if (library != null) text = library.name
                        }
                    }
                    field("Platform") {
                        isDisable = library != null
                        model.platformProperty.value = library?.platform ?: Platform.pc
                        model.platformProperty.onChange {
                            model.validate()
                        }
                        popoverComboMenu(
                            possibleItems = Platform.values().toList().observable(),
                            selectedItemProperty = model.platformProperty,
                            arrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                            styleClass = Style.platformItem,
                            itemStyleClass = Style.platformItem,
                            text = Platform::key,
                            graphic = { it.toLogo() }
                        )
                    }
                }
            }
        }
    }

    init {
        model.validate(decorateErrors = false)
    }

    override fun onDock() {
        if (library == null) browse()
    }

    private fun browse() {
        val directory = chooseDirectory("Browse Library Path...", initialDirectory = settings.prevDirectory?.existsOrNull()) ?: return
        settings.prevDirectory = directory
        model.path = directory.path
        model.name = directory.name
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): Choice {
        openModal(block = true)
        return if (accept && model.commit()) {
            if (library == null) {
                Choice.AddNewSource(model.toRequest())
            } else {
                Choice.EditSource(library.copy(path = model.path.toFile(), data = library.data.copy(name = model.name)))
            }
        } else {
            Choice.Cancel
        }
    }

    sealed class Choice {
        data class AddNewSource(val request: AddLibraryRequest) : Choice()
        data class EditSource(val library: Library) : Choice()
        object Cancel : Choice()
    }

    private class SourceViewModel : ViewModel() {
        val pathProperty = bind { SimpleStringProperty() }
        var path by pathProperty

        val nameProperty = bind { SimpleStringProperty() }
        var name by nameProperty

        val platformProperty = bind { SimpleObjectProperty<Platform>() }
        var platform by platformProperty

        fun toRequest() = AddLibraryRequest(
            path = path.toFile(),
            data = LibraryData(
                platform = platform,
                name = name
            )
        )

        override fun toString() = "SourceViewModel(name = $name, platform = $platform, path = $path)"
    }

    class Style : Stylesheet() {
        companion object {
            val platformItem by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            platformItem {
                prefWidth = 100.px
                alignment = Pos.CENTER_LEFT
            }
        }
    }
}