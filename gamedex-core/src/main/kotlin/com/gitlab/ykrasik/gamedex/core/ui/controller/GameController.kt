package com.gitlab.ykrasik.gamedex.core.ui.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ui.model.GamesModel
import tornadofx.Controller

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
class GameController : Controller() {
    private val model: GamesModel by di()

    fun delete(game: Game) {
        model.delete(game)
    }

    fun filterGenres() {
        TODO()  // TODO: Implement
    }

    fun filterLibraries() {
        TODO()  // TODO: Implement
    }
}