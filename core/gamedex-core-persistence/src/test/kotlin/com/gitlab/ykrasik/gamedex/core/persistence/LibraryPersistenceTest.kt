/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.persistence.AbstractPersistenceTest.LibraryScope
import com.gitlab.ykrasik.gamedex.test.randomPath
import com.gitlab.ykrasik.gamedex.util.toFile
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.jetbrains.exposed.exceptions.ExposedSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:50
 */
class LibraryPersistenceTest : AbstractPersistenceTest<LibraryScope>() {
    override fun scope() = LibraryScope()

    init {
        "Insert" should {
            "insert and retrieve a single library" test {
                val data = libraryData()

                val library = persistenceService.insertLibrary(data)

                library.data shouldBe data

                fetchLibraries() shouldBe listOf(library)
            }

            "insert and retrieve multiple libraries" test {
                val library1 = insertLibrary()
                val library2 = insertLibrary()

                fetchLibraries() shouldBe listOf(library1, library2)
            }

            "throw an exception when trying to insert a library at the same path twice" test {
                val path = randomPath()
                val library = givenLibrary(path = path)

                shouldThrow<ExposedSQLException> {
                    insertLibrary(path = path)
                }

                fetchLibraries() shouldBe listOf(library)
            }
        }

        "Update" should {
            "update a library's data" test {
                val library = givenLibrary(platform = Platform.pc)
                val updatedLibrary = library.copy(
                    data = library.data.copy(
                        name = library.name + "a",
                        path = (library.path.toString() + "b").toFile(),
                        platform = Platform.android
                    )
                )

                persistenceService.updateLibrary(updatedLibrary) shouldBe true

                fetchLibraries() shouldBe listOf(updatedLibrary)
            }

            "not update a library that doesn't exist" test {
                val library = givenLibrary()

                persistenceService.updateLibrary(library.copy(id = library.id + 1)) shouldBe false

                fetchLibraries() shouldBe listOf(library)
            }

            "throw an exception when trying to update a library's path to one that already exists" test {
                val library1 = givenLibrary()
                val library2 = givenLibrary()

                val updatedLibrary = library2.copy(data = library2.data.copy(path = library1.path))

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateLibrary(updatedLibrary)
                }

                fetchLibraries() shouldBe listOf(library1, library2)
            }
        }

        "Delete" should {
            "delete existing libraries" test {
                val library1 = givenLibrary()
                val library2 = givenLibrary()

                persistenceService.deleteLibrary(library1.id)
                fetchLibraries() shouldBe listOf(library2)

                persistenceService.deleteLibrary(library2.id)
                fetchLibraries() shouldBe emptyList<Library>()
            }

            "not delete a library that doesn't exist" test {
                val library = givenLibrary()

                persistenceService.deleteLibrary(library.id + 1) shouldBe false

                fetchLibraries() shouldBe listOf(library)
            }
        }

        "BatchDelete" should {
            "batch delete libraries by id" test {
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