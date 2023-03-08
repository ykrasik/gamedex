/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.persistence.AbstractPersistenceTest.GameScope
import com.gitlab.ykrasik.gamedex.test.randomPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.exceptions.ExposedSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:49
 */
class GamePersistenceTest : AbstractPersistenceTest<GameScope>() {
    override fun scope() = GameScope()

    init {
        describe("Insert") {
            itShould("insert and retrieve a single game with userData") {
                val metadata = randomMetadata()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = randomUserData()

                val game = insertGame(metadata, providerData, userData)
                game.metadata shouldBe metadata
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            itShould("insert and retrieve a single game with provider-override userData") {
                val userData = UserData(overrides = randomProviderOverrides())

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            itShould("insert and retrieve a single game with custom-override userData") {
                val userData = UserData(overrides = randomCustomOverrides())

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            itShould("insert and retrieve a single game with empty userData") {
                val userData = UserData()

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                fetchGames() shouldBe listOf(game)
            }

            itShould("insert and retrieve a single game with null userData") {
                val game = insertGame(userData = UserData.Null)
                game.userData shouldBe UserData.Null

                fetchGames() shouldBe listOf(game)
            }

            itShould("insert and retrieve multiple games") {
                val game1 = insertGame()
                val game2 = insertGame()

                fetchGames() shouldBe listOf(game1, game2)
            }

            itShould("throw an exception when trying to insert a game for an already existing path in the same library") {
                val path = randomPath()
                givenGame(path = path)

                shouldThrow<ExposedSQLException> {
                    insertGame(path = path)
                }
            }

            itShould("allow inserting a game for an already existing path in a different library") {
                val path = randomPath()
                val game1 = givenGame(path = path)
                val library2 = givenLibrary()

                val game2 = insertGame(library = library2, path = path)

                fetchGames() shouldBe listOf(game1, game2)
            }

            itShould("throw an exception when trying to insert a game for a non-existing library") {
                val library = givenLibrary()

                shouldThrow<ExposedSQLException> {
                    insertGame(library = library.copy(id = library.id + 1))
                }
            }
        }

        describe("Update") {
            itShould("update game") {
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

            itShould("update game user data with provider overrides") {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData(overrides = randomProviderOverrides()))
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            itShould("update game user data with custom overrides") {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData(overrides = randomCustomOverrides()))
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            itShould("update game user data to empty") {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData())
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            itShould("update game user data to null") {
                val game = givenGame()

                val updatedGame = game.copy(userData = UserData.Null)
                persistenceService.updateGame(updatedGame) shouldBe true

                fetchGames() shouldBe listOf(updatedGame)
            }

            itShould("update game metadata to a different library") {
                val library2 = givenLibrary()
                val game = givenGame()

                val updatedGame = game.withMetadata(randomMetadata(library = library2))
                persistenceService.updateGame(updatedGame) shouldBe true

                // Create date is not updated.
                fetchGames() shouldBe listOf(updatedGame.withMetadata { it.withCreateDate(game.metadata.createDate) })
            }

            itShould("not update a game that doesn't exist") {
                val game = givenGame()

                persistenceService.updateGame(game.copy(id = game.id + 1)) shouldBe false

                fetchGames() shouldBe listOf(game)
            }

            itShould("throw an exception when trying to update a game's path to one that already exists in the same library") {
                val game1 = givenGame(library)
                val game2 = givenGame(library)

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateGame(game1.withMetadata { it.copy(path = game2.metadata.path) })
                }

                fetchGames() shouldBe listOf(game1, game2)
            }

            itShould("allow updating a game's path to one that already exists in a different library") {
                val path = randomPath()
                val game1 = givenGame()
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2, path = path)

                val updatedGame1 = game1.withMetadata { it.copy(path = path) }
                persistenceService.updateGame(updatedGame1) shouldBe true

                fetchGames() shouldBe listOf(updatedGame1, game2)
            }

            itShould("throw an exception when trying to update a game to a different library, but the game's path already exists under that library") {
                val path = randomPath()
                val game1 = givenGame(path = path)
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2, path = path)

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateGame(game1.withMetadata(randomMetadata(library = library2, path = path)))
                }

                fetchGames() shouldBe listOf(game1, game2)
            }

            itShould("throw an exception when trying to update a game to a non-existing library") {
                val game = givenGame()

                shouldThrow<ExposedSQLException> {
                    persistenceService.updateGame(game.withMetadata(randomMetadata(library = library.copy(id = library.id + 1))))
                }
            }
        }

        describe("Delete") {
            itShould("delete existing games") {
                val game1 = givenGame()
                val game2 = givenGame()

                persistenceService.deleteGame(game1.id) shouldBe true
                fetchGames() shouldBe listOf(game2)

                persistenceService.deleteGame(game2.id) shouldBe true
                fetchGames() shouldBe emptyList<Game>()
            }

            itShould("not delete a game that doesn't exist") {
                val game = givenGame()

                persistenceService.deleteGame(game.id + 1) shouldBe false

                fetchGames() shouldBe listOf(game)
            }

            @Suppress("UNUSED_VARIABLE")
            itShould("delete all games belonging to a library when that library is deleted") {
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

        describe("BatchDelete") {
            itShould("batch delete games by id") {
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
            itShould("delete all games belonging to a library when that library is deleted") {
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

        describe("ClearUserData") {
            itShould("set userData of all games to null") {
                val game1 = givenGame(userData = randomUserData())
                val game2 = givenGame(userData = randomUserData())

                val library2 = givenLibrary()
                val game3 = givenGame(library = library2, userData = randomUserData())
                val game4 = givenGame(library = library2, userData = UserData.Null)

                persistenceService.clearUserData()
                fetchGames() shouldBe listOf(
                    game1.copy(userData = UserData.Null),
                    game2.copy(userData = UserData.Null),
                    game3.copy(userData = UserData.Null),
                    game4
                )
            }
        }
    }
}
