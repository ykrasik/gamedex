package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.datamodel.Game
import com.github.ykrasik.gamedex.common.datamodel.GamePlatform
import com.github.ykrasik.gamedex.common.datamodel.Library
import com.github.ykrasik.gamedex.common.datamodel.LibraryData
import com.github.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 11:15
 */
class LibraryPersistenceTest : PersistenceTest() {
    init {
        "Insert and retrieve libraries" {
            val library1 = givenLibraryExists(1)
            val library2 = givenLibraryExists(2)

            persistenceService.fetchAllLibraries() shouldBe listOf(library1, library2)
        }

        "Delete existing libraries along with all their games" {
            val library1 = givenLibraryExists(1)
            givenGameExists(1, library1)
            givenGameExists(2, library1)

            val library2 = givenLibraryExists(2)
            val game3 = givenGameExists(3, library2)

            persistenceService.deleteLibrary(1)
            persistenceService.fetchAllLibraries() shouldBe listOf(library2)
            persistenceService.fetchAllGames() shouldBe listOf(game3)

            persistenceService.deleteLibrary(2)
            persistenceService.fetchAllLibraries() shouldBe emptyList<Library>()
            persistenceService.fetchAllGames() shouldBe emptyList<Game>()
        }

        "Throw an exception when trying to delete a library that doesn't exist" {
            givenLibraryExists(1)

            shouldThrow<IllegalArgumentException> {
                persistenceService.deleteLibrary(2)
            }
        }

        "Throw an exception when trying to insert a library at the same path twice" {
            val path = "somePath"
            givenLibraryExists(1, path, GamePlatform.pc, "library1")

            shouldThrow<JdbcSQLException> {
                persistenceService.insert(AddLibraryRequest(path.toFile(), LibraryData(GamePlatform.xbox360, "library2")))
            }
        }
    }
}