package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.Library
import com.gitlab.ykrasik.gamedex.persistence.dao.GameDao
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.getValue
import tornadofx.observable
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 19:18
 */
@Singleton
class GamesModel @Inject constructor(
    private val gameDao: GameDao
) {
    val allProperty = SimpleListProperty(gameDao.all.observable())
    val all: ObservableList<Game> by allProperty

    fun contains(path: Path): Boolean = all.any { it.path == path }

    fun add(gameData: GameData, path: Path, library: Library): Game {
        val game = gameDao.add(gameData, path, library)
        all += game
        return game
    }

    fun delete(game: Game) {
        gameDao.delete(game)
        check(all.remove(game)) { "Error! Didn't contain game: $game" }
    }

    fun deleteByLibrary(library: Library) {
        gameDao.deleteByLibrary(library)
        all.removeAll { it.library == library }
    }

    fun getByLibrary(library: Library): ObservableList<Game> = all.filtered { it.library == library }
}