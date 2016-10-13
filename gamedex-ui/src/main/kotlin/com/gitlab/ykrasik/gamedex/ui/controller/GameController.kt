package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.SimpleListProperty
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.observable

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
class GameController : Controller() {
    private val persistenceService: PersistenceService by di()

    val gamesProperty = SimpleListProperty(persistenceService.games.all.observable())
    val games by gamesProperty

    fun filterGenres() {
        TODO()  // TODO: Implement
    }

    fun filterLibraries() {
        TODO()  // TODO: Implement
    }

    fun delete() {
        TODO()  // TODO: Implement
    }

    fun refresh() {
        TODO("refresh")  // TODO: Implement
    }

    fun deleteByLibrary(library: Library) {
        persistenceService.games.deleteByLibrary(library)
        games.removeAll { it.library == library }
    }
}