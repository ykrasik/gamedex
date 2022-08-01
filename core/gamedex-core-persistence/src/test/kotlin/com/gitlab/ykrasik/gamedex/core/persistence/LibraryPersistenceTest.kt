/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.persistence

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.LibraryType
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.persistence.AbstractPersistenceTest.LibraryScope
import com.gitlab.ykrasik.gamedex.test.randomPath
import com.gitlab.ykrasik.gamedex.util.file
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.exceptions.ExposedSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:50
 */
class LibraryPersistenceTest : AbstractPersistenceTest<LibraryScope>() {
    override fun scope() = LibraryScope()

    init {
        describe("Insert") {
            itShould("insert and retrieve a single library") {
                val data = libraryData()

                val library = persistenceService.insertLibrary(data)

                library.data shouldBe data

                fetchLibraries() shouldBe listOf(library)
            }

            itShould("insert and retrieve multiple libraries") {
                val library1 = insertLibrary()
                val library2 = insertLibrary()

                fetchLibraries() shouldBe listOf(library1, library2)
            }

            itShould("throw an exception when trying to insert a library at the same path twice") {
                val path = randomPath()
                val library = givenLibrary(path = path)

                shouldThrow<ExposedSQLException> {
                    insertLibrary(path = path)
                }

                fetchLibraries() shouldBe listOf(library)
            }
        }

        describe("Update") {
            itShould("update a library's data") {
                val library = givenLibrary(platform = Platform.Windows)
                val updatedLibrary = library.copy(
                    data = library.data.copy(
                        name = library.name + "a",
                        path = (library.path.toString() + "b").file,
                        type = LibraryType.Excluded,
                        platform = Platform.Android
                    )
                )

                persistenceService.updateLibrary(updatedLibrary) shouldBe true

                fetchLibraries() shouldBe listOf(updatedLibrary)
            }

            itShould("not update a library that doesn't exist") {
                val library = givenLibrary()

                persistenceService.updateLibrary(library.copy(id = library.id + 1)) shouldBe false

                fetchLibraries() shouldBe listOf(library)
            }

            itShould("throw an exception when trying to update a library's path to one that already exists") {
                val library1 = givenLibrary()
                val library2 = givenLibrary()

                val updatedLibrary = library2.copy(data = library2.data.copy(path = library1.path))

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateLibrary(updatedLibrary)
                }

                fetchLibraries() shouldBe listOf(library1, library2)
            }
        }

        describe("Delete") {
            itShould("delete existing libraries") {
                val library1 = givenLibrary()
                val library2 = givenLibrary()

                persistenceService.deleteLibrary(library1.id)
                fetchLibraries() shouldBe listOf(library2)

                persistenceService.deleteLibrary(library2.id)
                fetchLibraries() shouldBe emptyList<Library>()
            }

            itShould("not delete a library that doesn't exist") {
                val library = givenLibrary()

                persistenceService.deleteLibrary(library.id + 1) shouldBe false

                fetchLibraries() shouldBe listOf(library)
            }
        }

        describe("BatchDelete") {
            itShould("batch delete libraries by id") {
                val library1 = givenLibrary()
                val library2 = givenLibrary()
                val library3 = givenLibrary()
                val library4 = givenLibrary()

                persistenceService.deleteLibraries(emptyList()) shouldBe 0
                fetchLibraries() shouldBe listOf(library1, library2, library3, library4)

                persistenceService.deleteLibraries(listOf(999)) shouldBe 0
                fetchLibraries() shouldBe listOf(library1, library2, library3, library4)

                persistenceService.deleteLibraries(listOf(library1.id, library3.id, 999)) shouldBe 2
                fetchLibraries() shouldBe listOf(library2, library4)

                persistenceService.deleteLibraries(listOf(library2.id)) shouldBe 1
                fetchLibraries() shouldBe listOf(library4)

                persistenceService.deleteLibraries(listOf(library4.id)) shouldBe 1
                fetchLibraries() shouldBe emptyList<Game>()

                persistenceService.deleteLibraries(listOf(library4.id)) shouldBe 0
                fetchLibraries() shouldBe emptyList<Game>()
            }
        }
    }
}
