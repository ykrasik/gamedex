package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.core.UserPreferencesService
import com.gitlab.ykrasik.gamedex.ui.model.LibraryDataModel
import com.gitlab.ykrasik.gamedex.ui.util.chooseDirectory
import com.gitlab.ykrasik.gamedex.ui.util.enumComboBox
import tornadofx.*

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class AddLibraryFragment : Fragment("Add Library") {
    private val userPreferencesService: UserPreferencesService by di()

    private val libraryData = LibraryDataModel()
    private var save = false

    override val root = form {
        minWidth = 600.0

        fieldset("Add Library") {
            field("Path") {
                textfield().bind(libraryData.pathProperty())
                button("Browse") { setOnAction { browse() } }
            }
            field("Name") {
                textfield().bind(libraryData.nameProperty())
            }
            field("Platform") { enumComboBox<GamePlatform>(libraryData.platformProperty()) { value = GamePlatform.PC } }
        }

        button("Add") {
            isDefaultButton = true

            disableProperty().bind(libraryData.notAllFieldsSet)
            setOnAction {
                save = true
                closeModal()
            }
        }
    }

    private fun browse() {
        val path = chooseDirectory("Add Library") {
            initialDirectory = userPreferencesService.preferences.pevDirectory?.toFile()
        } ?: return

        userPreferencesService.update { pevDirectory = path }
        libraryData.path = path.toString()
        libraryData.name = path.fileName.toString()
    }

    fun show(): LibraryData? {
        openModal(block = true)
        return if (save && !libraryData.notAllFieldsSet.value) {
            libraryData.toLibraryData()
        } else {
            null
        }
    }
}