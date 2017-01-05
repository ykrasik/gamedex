package com.gitlab.ykrasik.gamedex.core.ui.view.fragment

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.core.model.LibraryDataModel
import com.gitlab.ykrasik.gamedex.core.ui.chooseDirectory
import com.gitlab.ykrasik.gamedex.core.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.core.util.UserPreferences
import tornadofx.*

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 10:56
 */
class AddLibraryFragment : Fragment("Add Library") {
    private val preferences: UserPreferences by di()

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
        val directory = chooseDirectory("Add Library") {
            initialDirectoryProperty().bind(preferences.prevDirectoryProperty)
        } ?: return
        preferences.prevDirectory = directory
        libraryData.path = directory.path
        libraryData.name = directory.name
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