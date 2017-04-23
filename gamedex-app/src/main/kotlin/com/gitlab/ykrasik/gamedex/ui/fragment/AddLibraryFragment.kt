package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.GamePlatform
import com.gitlab.ykrasik.gamedex.LibraryData
import com.gitlab.ykrasik.gamedex.core.UserPreferences
import com.gitlab.ykrasik.gamedex.repository.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.util.existsOrNull
import com.gitlab.ykrasik.gamedex.util.toFile
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.ButtonBar
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.*
import java.io.File

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class AddLibraryFragment : Fragment("Add Library") {
    private val preferences: UserPreferences by di()

    private val model = AddLibraryViewModel()
    private var accept = false

    override val root = form {
        minWidth = 600.0

        fieldset("Add Library") {
            field("Path") {
                textfield(model.pathProperty).validator {
                    if (it.isNullOrBlank()) error("Path is required!")
                    else if (!File(it).isDirectory) error("Path doesn't exist!")
                    else null
                }
                button("Browse") { setOnAction { browse() } }
            }
            field("Name") { textfield(model.nameProperty).required() }
            field("Platform") { enumComboBox<GamePlatform>(model.platformProperty) { value = GamePlatform.pc } }
        }

        buttonbar {
            button("Add", type = ButtonBar.ButtonData.OK_DONE) {
                isDefaultButton = true
                enableWhen { model.valid }
                setOnAction { close(accept = true) }
            }
            cancelButton { setOnAction { close(accept = false) } }
        }
    }
    
    init {
        model.validate(decorateErrors = false)
    }

    private fun browse() {
        val directory = chooseDirectory("Browse Library Path...", initialDirectory = preferences.prevDirectory?.existsOrNull()) ?: return
        preferences.prevDirectory = directory
        model.path = directory.path
        model.name = directory.name
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    suspend fun show(): AddLibraryRequest? = run(JavaFx) {
        openModal(block = true)
        if (accept && model.commit()) {
            model.toRequest()
        } else {
            null
        }
    }
}

private class AddLibraryViewModel : ViewModel() {
    val pathProperty = bind { SimpleStringProperty() }
    var path by pathProperty

    val nameProperty = bind { SimpleStringProperty() }
    var name by nameProperty

    val platformProperty = bind { SimpleObjectProperty<GamePlatform>() }
    var platform by platformProperty

    fun toRequest() = AddLibraryRequest(
        path = path.toFile(),
        data = LibraryData(
            platform = platform,
            name = name
        )
    )

    override fun toString() = "AddLibraryViewModel(name = $name, platform = $platform, path = $path)"
}