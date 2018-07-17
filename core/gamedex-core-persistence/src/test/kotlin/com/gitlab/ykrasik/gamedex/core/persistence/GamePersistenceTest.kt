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
import com.gitlab.ykrasik.gamedex.UserData
import com.gitlab.ykrasik.gamedex.test.randomPath
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.jetbrains.exposed.exceptions.ExposedSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:49
 */
class GamePersistenceTest : AbstractPersistenceTest() {
    init {
        "Insert" should {
            "insert and retrieve a single game with userData" test {
                val metadata = randomMetadata()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = randomUserData()

                val game = insertGame(metadata, providerData, userData)
                game.metadata shouldBe metadata
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with provider-override userData" test {
                val userData = UserData(overrides = randomProviderOverrides())

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with custom-override userData" test {
                val userData = UserData(overrides = randomCustomOverrides())

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with empty userData" test {
                val userData = UserData()

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with null userData" test {
                val game = insertGame(userData = null)
                game.userData shouldBe null

                fetchGames() shouldBe listOf(game)
            }

            "insert and retrieve multiple games" test {
                val game1 = insertGame()
                val game2 = insertGame()

                fetchGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to insert a game for an already existing path in the same library" test {
                val path = randomPath()
                givenGame(path = path)

                shouldThrow<ExposedSQLException> {
                    insertGame(path = path)
                }
            }

            "allow inserting a game for an already existing path in a different library" test {
                val path = randomPath()
                val game1 = givenGame(path = path)
                val library2 = givenLibrary()

                val game2 = insertGame(library = library2, path = path)

                fetchGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to insert a game for a non-existing library" test {
                val library = givenLibrary()

                shouldThrow<ExposedSQLException> {
                    insertGame(library = library.copy(id = library.id + 1))
                }
            }
        }

        "Update" should {
            "update game" test {
                val game = givenGame()
                val updatedGame = game.copy(
                    metadata = randomMetadata(),
                    providerData = listOf(randomProviderData()),
                    userData = randomUserData()
                )

                persistenceService.updateGame(updatedGame) shouldBe true

                // Create date is not updated.
                fetchGames() shouldBe listOf(updatedGame.withMetadata { it.withCreateDate(game.metadata.createDate) })
            }

            "update game user data with provider overrides" test {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData(overrides = randomProviderOverrides()))
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            "update game user data with custom overrides" test {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData(overrides = randomCustomOverrides()))
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            "update game user data to empty" test {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData())
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            "update game user data to null" test {
                val game = givenGame()

                val updatedGame = game.copy(userData = null)
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            "update game metadata to a different library" test {
                val library2 = givenLibrary()
                val game = givenGame()

                val updatedGame = game.withMetadata(randomMetadata(library = library2))
                persistenceService.updateGame(updatedGame) shouldBe true

                // Create date is not updated.
                fetchGames() shouldBe listOf(updatedGame.withMetadata { it.withCreateDate(game.metadata.createDate) })
            }

            "not update a game that doesn't exist" test {
                val game = givenGame()

                persistenceService.updateGame(game.copy(id = game.id + 1)) shouldBe false

                fetchGames() shouldBe listOf(game)
            }

            "throw an exception when trying to update a game's path to one that already exists in the same library" test {
                val game1 = givenGame(library)
                val game2 = givenGame(library)

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateGame(game1.withMetadata { it.copy(path = game2.metadata.path) })
                }

                fetchGames() shouldBe listOf(game1, game2)
            }

            "allow updating a game's path to one that already exists in a different library" test {
                val path = randomPath()
                val game1 = givenGame()
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2, path = path)

                val updatedGame1 = game1.withMetadata { it.copy(path = path) }
                persistenceService.updateGame(updatedGame1) shouldBe true

                fetchGames() shouldBe listOf(updatedGame1, game2)
            }

            "throw an exception when trying to update a game to a different library, but the game's path already exists under that library" test {
                val path = randomPath()
                val game1 = givenGame(path = path)
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2, path = path)

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateGame(game1.withMetadata(randomMetadata(library = library2, path = path)))
                }

                fetchGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to update a game to a non-existing library" test {
                val game = givenGame()

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateGame(game.withMetadata(randomMetadata(library = library.copy(id = library.id + 1))))
                }
            }
        }

        "Delete" should {
            "delete existing games" test {
                val game1 = givenGame()
                val game2 = givenGame()

                persistenceService.deleteGame(game1.id) shouldBe true
                fetchGames() shouldBe listOf(game2)

                persistenceService.deleteGame(game2.id) shouldBe true
                fetchGames() shouldBe emptyList<Game>()
            }

            "not delete a game that doesn't exist" test {
                val game = givenGame()

                persistenceService.deleteGame(game.id + 1) shouldBe false

                fetchGames() shouldBe listOf(game)
            }

            @Suppress("UNUSED_VARIABLE")
            "delete all games belonging to a library when that library is deleted" test {
                val game1 = givenGame(library = library)
                val game2 = givenGame(library = library)

                val library2 = givenLibrary()
                val game3 = givenGame(library = library2)
                val game4 = givenGame(library = library2)

                persistenceService.deleteLibrary(library.id)
                fetchGames() shouldBe listOf(game3, game4)

                persistenceService.deleteLibrary(library2.id)
                fetchGames() shouldBe emptyList<Game>()
            }
        }

        "BatchDelete" should {
            "batch delete games by id" test {
                val game1 = givenGame(library = library)
                val game2 = givenGame(library = library)

                val library2 = givenLibrary()
                val game3 = givenGame(library = library2)
                val game4 = givenGame(library = library2)

                persistenceService.deleteGames(emptyList()) shouldBe 0
                fetchGames() shouldBe listOf(game1, game2, game3, game4)

                persistenceService.deleteGames(listOf(999)) shouldBe 0
                fetchGames() shouldBe listOf(game1, game2, game3, game4)

                persistenceService.deleteGames(listOf(game1.id, game3.id, 999)) shouldBe 2
                fetchGames() shouldBe listOf(game2, game4)

                persistenceService.deleteGames(listOf(game2.id)) shouldBe 1
                fetchGames() shouldBe listOf(game4)

                persistenceService.deleteGames(listOf(game4.id)) shouldBe 1
                fetchGames() shouldBe emptyList<Game>()

                persistenceService.deleteGames(listOf(game4.id)) shouldBe 0
                fetchGames() shouldBe emptyList<Game>()
            }

            @Suppress("UNUSED_VARIABLE")
            "delete all games belonging to a library when that library is deleted" test {
                val game1 = givenGame(library = library)
                val game2 = givenGame(library = library)

                val library2 = givenLibrary()
                val game3 = givenGame(library = library2)
                val game4 = givenGame(library = library2)

                persistenceService.deleteLibraries(listOf(library.id))
                fetchGames() shouldBe listOf(game3, game4)

                persistenceService.deleteLibraries(listOf(library2.id))
                fetchGames() shouldBe emptyList<Game>()
            }
        }

        "ClearUserData" should {
            "set userData of all games to null" test {
                val game1 = givenGame(userData = randomUserData())
                val game2 = givenGame(userData = randomUserData())

                val library2 = givenLibrary()
                val game3 = givenGame(library = library2, userData = randomUserData())
                val game4 = givenGame(library = library2, userData = null)

                persistenceService.clearUserData()
                fetchGames() shouldBe listOf(game1.copy(userData = null), game2.copy(userData = null), game3.copy(userData = null), game4)
            }
        }
    }

    private infix fun String.test(test: GameScope.() -> Unit) = inScope(::GameScope, test)
}