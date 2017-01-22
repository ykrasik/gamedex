package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.LibraryData
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 11:15
 */
class LibraryDaoTest : DaoTest() {
    val dao = libraryDao

    init {
        "Insert and check that a library for this path exists" {
            givenLibraryExists(1, "path/to/library")

            dao.exists("path/to/library".toPath()) shouldBe true
            dao.exists("path/to/library2".toPath()) shouldBe false
        }

        "Insert and retrieve libraries" {
            val library1 = givenLibraryExists(1, "library1")
            val library2 = givenLibraryExists(2, "library2")

            dao.get(1.toId()) shouldBe library1
            dao.get(2.toId()) shouldBe library2
        }

        "Retrieve all existing libraries" {
            val library1 = givenLibraryExists(1, "library1")
            val library2 = givenLibraryExists(2, "library2")
            val library3 = givenLibraryExists(3, "library3")

            dao.all shouldBe listOf(library1, library2, library3)
        }

        // FIXME: This will fail if the game is linked to a genre.
        "Delete existing libraries along with all their games" {
            val library1 = givenLibraryExists(1, "library1")
            val game1 = givenGameExists(1, library1)
            val game2 = givenGameExists(2, library1)

            val library2 = givenLibraryExists(2, "library2")
            val game3 = givenGameExists(3, library2)

            dao.delete(library1)
            gameDao.all shouldBe listOf(game3)
            dao.all shouldBe listOf(library2)

            dao.delete(library2)
            gameDao.all shouldBe emptyList<Game>()
            dao.all shouldBe emptyList<Library>()
        }

        "Throw an exception when trying to fetch a non-existing library" {
            givenLibraryExists(1, "library1")

            shouldThrow<IllegalArgumentException> {
                dao.get(2.toId())
            }
        }

        "Throw an exception when trying to delete a library that doesn't exist" {
            givenLibraryExists(1, "library1")

            shouldThrow<IllegalArgumentException> {
                val invalidLibrary = Library(2.toId(), "".toPath(), "", GamePlatform.pc)
                dao.delete(invalidLibrary)
            }
        }

        "Throw an exception when trying to insert a library at the same path twice" {
            givenLibraryExists(1, "path1", GamePlatform.pc, "library1")

            shouldThrow<JdbcSQLException> {
                dao.add(LibraryData("path1".toPath(), "library2", GamePlatform.xbox360))
            }
        }
    }
}