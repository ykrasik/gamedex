package com.gitlab.ykrasik.gamedex.core.ui.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ui.model.GameRepository
import tornadofx.Controller

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
// TODO: This class is redundant, the logic can sit in the view.
class GameController : Controller() {
    private val gameRepository: GameRepository by di()

    fun delete(game: Game) {
        gameRepository.delete(game)
    }

    fun filterGenres() {
        TODO()  // TODO: Implement
    }

    fun filterLibraries() {
        TODO()  // TODO: Implement
    }
}