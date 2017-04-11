package com.gitlab.ykrasik.gamedex.ui.controller

import com.gitlab.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.ui.areYouSureDialog
import com.gitlab.ykrasik.gamedex.ui.model.GameRepository
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
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
        if (confirmDelete(game)) {
            launch(CommonPool) {
                gameRepository.delete(game)
            }
        }
    }

    private fun confirmDelete(game: Game): Boolean = areYouSureDialog("Delete game '${game.name}'?")

    fun filterGenres() {
        TODO()  // TODO: Implement
    }

    fun filterLibraries() {
        TODO()  // TODO: Implement
    }
}