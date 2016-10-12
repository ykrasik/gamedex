package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.core.LibraryService
import com.gitlab.ykrasik.gamedex.ui.view.fragment.AddLibraryFragment
import javafx.beans.property.SimpleListProperty
import tornadofx.Controller
import tornadofx.observable

/**
 * User: ykrasik
 * Date: 10/10/2016
 * Time: 13:25
 */
class LibraryController : Controller() {
    private val libraryService: LibraryService by di()

    private val libraries = libraryService.all.observable()
    val librariesProperty = SimpleListProperty<Library>(libraries)

    fun add(): Boolean {
        val libraryData = AddLibraryFragment().show() ?: return false
        val library = libraryService.add(libraryData)
        libraries.add(library)
        return true
    }

    fun delete(library: Library) {
        libraryService.delete(library)
        libraries.remove(library)
    }
}