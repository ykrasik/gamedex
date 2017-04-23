package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.test.randomFile
import com.gitlab.ykrasik.gamedex.test.randomPath
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:50
 */
class LibraryPersistenceTest : AbstractPersistenceTest() {
    init {
        "Library persistence insert" should {
            "insert and retrieve a single game".inLazyScope({ LibraryScope() }) {
                val path = randomFile()
                val data = randomLibraryDate()

                val library = persistenceService.insertLibrary(path, data)

                library.path shouldBe path
                library.data shouldBe data

                persistenceService.fetchAllLibraries() shouldBe listOf(library)
            }

            "insert and retrieve libraries".inLazyScope({ LibraryScope() }) {
                val library1 = insertLibrary()
                val library2 = insertLibrary()

                persistenceService.fetchAllLibraries() shouldBe listOf(library1, library2)
            }

            "throw an exception when trying to insert a library at the same path twice".inLazyScope({ LibraryScope() }) {
                val path = randomPath()
                givenLibraryExists(path = path)

                shouldThrow<JdbcSQLException> {
                    insertLibrary(path = path)
                }
            }
        }

        "Library persistence delete" should {
            "delete existing libraries".inLazyScope({ LibraryScope() }) {
                val library1 = givenLibraryExists()
                val library2 = givenLibraryExists()

                persistenceService.deleteLibrary(library1.id)
                persistenceService.fetchAllLibraries() shouldBe listOf(library2)

                persistenceService.deleteLibrary(library2.id)
                persistenceService.fetchAllLibraries() shouldBe emptyList<Library>()
            }

            "throw an exception when trying to delete a library that doesn't exist".inLazyScope({ LibraryScope() }) {
                val library = givenLibraryExists()

                shouldThrow<IllegalArgumentException> {
                    persistenceService.deleteLibrary(library.id + 1)
                }
            }
        }
    }
}