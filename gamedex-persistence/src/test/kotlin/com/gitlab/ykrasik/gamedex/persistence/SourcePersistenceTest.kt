package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.test.randomFile
import com.gitlab.ykrasik.gamedex.test.randomPath
import com.gitlab.ykrasik.gamedex.util.toFile
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:50
 */
class SourcePersistenceTest : AbstractPersistenceTest() {
    init {
        "Library persistence insert" should {
            "insert and retrieve a single library".inLazyScope({ LibraryScope() }) {
                val path = randomFile()
                val data = libraryData()

                val library = persistenceService.insertLibrary(path, data)

                library.path shouldBe path
                library.data shouldBe data

                persistenceService.fetchAllLibraries() shouldBe listOf(library)
            }

            "insert and retrieve multiple libraries".inLazyScope({ LibraryScope() }) {
                val source1 = insertLibrary()
                val source2 = insertLibrary()

                persistenceService.fetchAllLibraries() shouldBe listOf(source1, source2)
            }

            "throw an exception when trying to insert a library at the same path twice".inLazyScope({ LibraryScope() }) {
                val path = randomPath()
                givenLibraryExists(path = path)

                shouldThrow<JdbcSQLException> {
                    insertLibrary(path = path)
                }
            }
        }

        "Library persistence update" should {
            "update a library's path & data".inLazyScope({ LibraryScope() }) {
                val library = givenLibraryExists(platform = Platform.pc)
                val updatedSource = library.copy(
                    path = (library.path.toString() + "a").toFile(),
                    data = library.data.copy(platform = Platform.android, name = library.name + "b"))

                persistenceService.updateLibrary(updatedSource)

                persistenceService.fetchAllLibraries() shouldBe listOf(updatedSource)
            }

            "throw an exception when trying to update a library's path to one that already exists".inLazyScope({ LibraryScope() }) {
                val source1 = givenLibraryExists()
                val source2 = givenLibraryExists()

                val updatedSource = source2.copy(path = source1.path)

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateLibrary(updatedSource)
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