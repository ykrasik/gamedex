package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.util.existsOrNull
import com.gitlab.ykrasik.gamedex.model.LibraryDataModel
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.util.UserPreferences
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
            field("Platform") { enumComboBox<GamePlatform>(libraryData.platformProperty()) { value = GamePlatform.pc } }
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
        val directory = chooseDirectory("Add Library", initialDirectory = preferences.prevDirectory?.existsOrNull()) ?: return
        preferences.prevDirectory = directory
        libraryData.path = directory.path
        libraryData.name = directory.name
    }

    fun show(): AddLibraryRequest? {
        openModal(block = true)
        return if (save && !libraryData.notAllFieldsSet.value) {
            libraryData.toRequest()
        } else {
            null
        }
    }
}