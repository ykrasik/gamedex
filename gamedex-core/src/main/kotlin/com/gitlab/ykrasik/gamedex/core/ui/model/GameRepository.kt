package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.getValue
import tornadofx.observable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:18
 */
@Singleton
class GameRepository @Inject constructor(
    private val persistenceService: PersistenceService
) {
    val gamesProperty: ListProperty<Game> = SimpleListProperty(persistenceService.games.all.observable())
    val games: ObservableList<Game> by gamesProperty

    fun contains(path: File): Boolean = games.any { it.path == path }

    fun add(gameData: GameData, imageData: GameImageData, path: File, libraryId: Int): Game {
        val game = persistenceService.games.add(gameData, imageData, path, libraryId)
        games += game
        return game
    }

    fun delete(game: Game) {
        persistenceService.games.delete(game)
        check(games.remove(game)) { "Error! Didn't contain game: $game" }
    }

    fun deleteByLibrary(libraryId: Int) {
        persistenceService.games.deleteByLibrary(libraryId)
        games.removeAll { it.libraryId == libraryId }
    }

    fun getByLibrary(libraryId: Int): ObservableList<Game> = games.filtered { it.libraryId == libraryId }
}