package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.randomPath
import com.gitlab.ykrasik.gamedex.util.toFile
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:49
 */
class GamePersistenceTest : AbstractPersistenceTest() {
    init {
        "Insert" should {
            "insert and retrieve a single game with userData".test {
                val metadata = randomMetadata()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = randomUserData()

                val game = insertGame(metadata, providerData, userData)
                game.metadata shouldBe metadata.copy(updateDate = nowMock)
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with provider-override userData".test {
                val userData = UserData(overrides = randomProviderOverrides())

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with custom-override userData".test {
                val userData = UserData(overrides = randomCustomOverrides())

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with empty userData".test {
                val userData = UserData()

                val game = insertGame(userData = userData)
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with null userData".test {
                val game = insertGame(userData = null)
                game.userData shouldBe null

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve multiple games".test {
                val game1 = insertGame()
                val game2 = insertGame()

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to insert a game for an already existing path in the same library".test {
                val path = randomPath()
                givenGame(path = path)

                shouldThrow<JdbcSQLException> {
                    insertGame(path = path)
                }
            }

            "allow inserting a game for an already existing path in a different library".test {
                val path = randomPath()
                val game1 = givenGame(path = path)
                val library2 = givenLibrary()

                val game2 = insertGame(library = library2, path = path)

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to insert a game for a non-existing library".test {
                val nonExistingLibrary = Library(2, "".toFile(), LibraryData(Platform.pc, ""))

                shouldThrow<JdbcSQLException> {
                    insertGame(library = nonExistingLibrary)
                }
            }
        }

        "Update" should {
            "update game".test {
                val game = givenGame()
                val updatedMetadata = randomMetadata()
                val updatedProviderData = listOf(randomProviderData())
                val updatedUserData = randomUserData()

                val updatedGame = persistenceService.updateGame(game.copy(
                    metadata = updatedMetadata,
                    providerData = updatedProviderData,
                    userData = updatedUserData
                ))
                updatedGame.metadata shouldBe updatedMetadata.copy(updateDate = nowMock)
                updatedGame.providerData shouldBe updatedProviderData
                updatedGame.userData shouldBe updatedUserData

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data with provider overrides".test {
                val game = givenGame()

                val updatedGame = persistenceService.updateGame(game.copy(userData = UserData(overrides = randomProviderOverrides())))

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data with custom overrides".test {
                val game = givenGame()

                val updatedGame = persistenceService.updateGame(game.copy(userData = UserData(overrides = randomCustomOverrides())))

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data to empty".test {
                val game = givenGame()

                val updatedGame = persistenceService.updateGame(game.copy(userData = UserData()))

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data to null".test {
                val game = givenGame()

                val updatedGame = persistenceService.updateGame(game.copy(userData = null))

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game metadata to a different library".test {
                val library2 = givenLibrary()
                val game = givenGame()

                val updatedGame = persistenceService.updateGame(game.withMetadata(randomMetadata(library = library2)))

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "throw an exception when trying to update a game of a non-existing id".test {
                val game = givenGame()

                shouldThrow<IllegalArgumentException> {
                    persistenceService.updateGame(game.copy(id = game.id + 1))
                }
            }

            "throw an exception when trying to update a game's path to one that already exists in the same library".test {
                val game1 = givenGame(library)
                val game2 = givenGame(library)

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateGame(game1.withMetadata { it.copy(path = game2.metadata.path) })
                }

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "allow updating a game's path to one that already exists in a different library".test {
                val path = randomPath()
                val game1 = givenGame()
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2, path = path)

                val updatedGame1 = persistenceService.updateGame(game1.withMetadata { it.copy(path = path) })

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame1, game2)
            }

            "throw an exception when trying to update a game to a different library, but the game's path already exists under that library".test {
                val path = randomPath()
                val game1 = givenGame(path = path)
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2, path = path)

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateGame(game1.withMetadata(randomMetadata(library = library2, path = path)))
                }

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to update a game to a non-existing library".test {
                val game = givenGame()
                val nonExistingLibrary = Library(2, "".toFile(), LibraryData(Platform.pc, ""))

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateGame(game.withMetadata(randomMetadata(library = nonExistingLibrary)))
                }
            }
        }

        "Delete" should {
            "delete existing games".test {
                val game1 = givenGame()
                val game2 = givenGame()

                persistenceService.deleteGame(game1.id)
                persistenceService.fetchAllGames() shouldBe listOf(game2)

                persistenceService.deleteGame(game2.id)
                persistenceService.fetchAllGames() shouldBe emptyList<Game>()
            }

            "throw an exception when trying to delete a non-existing game".test {
                val game = givenGame()

                shouldThrow<IllegalArgumentException> {
                    persistenceService.deleteGame(game.id + 1)
                }
            }

            @Suppress("UNUSED_VARIABLE")
            "delete all games belonging to a library when that library is deleted".test {
                val game1 = givenGame(library = library)
                val game2 = givenGame(library = library)

                val library2 = givenLibrary()
                val game3 = givenGame(library = library2)
                val game4 = givenGame(library = library2)

                persistenceService.deleteLibrary(library.id)
                persistenceService.fetchAllGames() shouldBe listOf(game3, game4)

                persistenceService.deleteLibrary(library2.id)
                persistenceService.fetchAllGames() shouldBe emptyList<Game>()
            }
        }
    }

    private fun String.test(test: GameScope.() -> Unit) = inScope({ GameScope() }, test)
}