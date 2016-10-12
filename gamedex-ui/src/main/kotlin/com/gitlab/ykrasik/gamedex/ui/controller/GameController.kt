package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.SimpleListProperty
import tornadofx.Controller
import tornadofx.getValue
import tornadofx.observable
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
class GameController : Controller() {
    private val persistenceService: PersistenceService by di()

    val gamesProperty = SimpleListProperty<Game>(persistenceService.games.all.observable())
    var games by gamesProperty
        private set

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
}