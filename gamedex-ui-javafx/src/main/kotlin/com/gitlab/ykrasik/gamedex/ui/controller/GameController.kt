package com.gitlab.ykrasik.gamedex.ui.controller

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import com.gitlab.ykrasik.gamedex.core.ui.GameUIManager
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.SimpleListProperty
import tornadofx.getValue
import tornadofx.observable
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 14:39
 */
@Singleton
class GameController @Inject constructor(
    private val persistenceService: PersistenceService
) : GameUIManager {
    val gamesProperty = SimpleListProperty(persistenceService.games.all.observable())
    override val all by gamesProperty

    override fun add(gameData: GameData, path: Path, library: Library): Game {
        val game = persistenceService.games.add(gameData, path, library)
        all += game
        return game
    }

    fun delete(game: Game) {
        persistenceService.games.delete(game)
        check(all.remove(game)) { "Error! Didn't contain game: $game" }
    }

    fun deleteByLibrary(library: Library) {
        persistenceService.games.deleteByLibrary(library)
        all.removeAll { it.library == library }
    }

    fun filterGenres() {
        TODO()  // TODO: Implement
    }

    fun filterLibraries() {
        TODO()  // TODO: Implement
    }
}