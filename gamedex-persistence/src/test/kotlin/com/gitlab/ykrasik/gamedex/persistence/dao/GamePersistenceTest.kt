package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.toFile
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 15:41
 */
class GamePersistenceTest : PersistenceTest() {
    lateinit var library: Library

    override fun beforeEach() {
        super.beforeEach()
        library = givenLibraryExists(1)
    }

    init {
        "Insert and retrieve games" {
            val game1 = givenGameExists(1, library)
            val game2 = givenGameExists(2, library)

            persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
        }

        "Delete a game" {
            val game1 = givenGameExists(1, library)
            val game2 = givenGameExists(2, library)

            persistenceService.deleteGame(1)
            persistenceService.fetchAllGames() shouldBe listOf(game2)

            persistenceService.deleteGame(2)
            persistenceService.fetchAllGames() shouldBe emptyList<Game>()
        }

        "Throw an exception when trying to insert a game for an already existing path" {
            val path = "path/to/game"
            givenGameExists(1, library, path)

            val library2 = givenLibraryExists(2)

            shouldThrow<JdbcSQLException> {
                // Too much typing... so we called this method again.
                givenGameExists(2, library2, path)
            }
        }

        "Throw an exception when trying to insert a game for a non-existing library" {
            val library = Library(2, "".toFile(), LibraryData(GamePlatform.pc, ""))

            shouldThrow<JdbcSQLException> {
                // Too much typing... so we called this method again.
                givenGameExists(1, library)
            }
        }

        "Throw an exception when trying to delete a non-existing game" {
            givenGameExists(1, library)

            shouldThrow<IllegalArgumentException> {
                persistenceService.deleteGame(2)
            }
        }
    }
}