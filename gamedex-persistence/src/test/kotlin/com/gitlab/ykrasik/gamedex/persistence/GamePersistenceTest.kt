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
        "Game persistence insert" should {
            "insert and retrieve a single game with userData".inLazyScope({ GameScope() }) {
                val metaData = randomMetaData()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = randomUserData()

                val game = persistenceService.insertGame(metaData, providerData, userData)

                game.metaData shouldBe metaData
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with provider-override userData".inLazyScope({ GameScope() }) {
                val metaData = randomMetaData()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = UserData(overrides = randomProviderOverrides())

                val game = persistenceService.insertGame(metaData, providerData, userData)

                game.metaData shouldBe metaData
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with custom-override userData".inLazyScope({ GameScope() }) {
                val metaData = randomMetaData()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = UserData(overrides = randomCustomOverrides())

                val game = persistenceService.insertGame(metaData, providerData, userData)

                game.metaData shouldBe metaData
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with empty userData".inLazyScope({ GameScope() }) {
                val metaData = randomMetaData()
                val providerData = listOf(randomProviderData(), randomProviderData())
                val userData = UserData()

                val game = persistenceService.insertGame(metaData, providerData, userData)

                game.metaData shouldBe metaData
                game.providerData shouldBe providerData
                game.userData shouldBe userData

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve a single game with null userData".inLazyScope({ GameScope() }) {
                val metaData = randomMetaData()
                val providerData = listOf(randomProviderData(), randomProviderData())

                val game = persistenceService.insertGame(metaData, providerData, userData = null)

                game.metaData shouldBe metaData
                game.providerData shouldBe providerData
                game.userData shouldBe null

                persistenceService.fetchAllGames() shouldBe listOf(game)
            }

            "insert and retrieve multiple games".inLazyScope({ GameScope() }) {
                val game1 = insertGame()
                val game2 = insertGame()

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to insert a game for an already existing path in the same library".inLazyScope({ GameScope() }) {
                val path = randomPath()
                givenGameExists(path = path)

                shouldThrow<JdbcSQLException> {
                    insertGame(path = path)
                }
            }

            "allow inserting a game for an already existing path in a different library".inLazyScope({ GameScope() }) {
                val path = randomPath()
                val game1 = givenGameExists(path = path)
                val library2 = givenLibraryExists()

                val game2 = insertGame(library = library2, path = path)

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to insert a game for a non-existing library".inLazyScope({ GameScope() }) {
                val nonExistingLibrary = Library(2, "".toFile(), LibraryData(Platform.pc, ""))

                shouldThrow<JdbcSQLException> {
                    insertGame(library = nonExistingLibrary)
                }
            }
        }

        "Game persistence update" should {
            "update game".inLazyScope({ GameScope() }) {
                val game = givenGameExists()
                val updatedGame = game.copy(
                    metaData = randomMetaData(),
                    providerData = listOf(randomProviderData()),
                    userData = randomUserData()
                )

                persistenceService.updateGame(updatedGame)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data with provider overrides".inLazyScope({ GameScope() }) {
                val game = givenGameExists()
                val updatedGame = game.copy(userData = UserData(overrides = randomProviderOverrides()))

                persistenceService.updateGame(updatedGame)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data with custom overrides".inLazyScope({ GameScope() }) {
                val game = givenGameExists()
                val updatedGame = game.copy(userData = UserData(overrides = randomCustomOverrides()))

                persistenceService.updateGame(updatedGame)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data to empty".inLazyScope({ GameScope() }) {
                val game = givenGameExists()
                val updatedGame = game.copy(userData = UserData())

                persistenceService.updateGame(updatedGame)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game user data to null".inLazyScope({ GameScope() }) {
                val game = givenGameExists()
                val updatedGame = game.copy(userData = null)

                persistenceService.updateGame(updatedGame)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "update game metaData to a different library".inLazyScope({ GameScope() }) {
                val library2 = givenLibraryExists()
                val game = givenGameExists()
                val updatedGame = game.copy(metaData = randomMetaData(library = library2))

                persistenceService.updateGame(updatedGame)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame)
            }

            "throw an exception when trying to update a game of a non-existing id".inLazyScope({ GameScope() }) {
                val game = givenGameExists()

                shouldThrow<IllegalArgumentException> {
                    persistenceService.updateGame(game.copy(id = game.id + 1))
                }
            }

            "throw an exception when trying to update a game's path to one that already exists in the same library".inLazyScope({ GameScope() }) {
                val game1 = givenGameExists(library)
                val game2 = givenGameExists(library)

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateGame(game1.copy(metaData = game1.metaData.copy(path = game2.metaData.path)))
                }

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "allow updating a game's path to one that already exists in a different library".inLazyScope({ GameScope() }) {
                val path = randomPath()
                val game1 = givenGameExists()
                val library2 = givenLibraryExists()
                val game2 = givenGameExists(library = library2, path = path)
                val updatedGame1 = game1.copy(metaData = game1.metaData.copy(path = path.toFile()))

                persistenceService.updateGame(updatedGame1)

                persistenceService.fetchAllGames() shouldBe listOf(updatedGame1, game2)
            }

            "throw an exception when trying to update a game to a different library, but the game's path already exists under that library".inLazyScope({ GameScope() }) {
                val path = randomPath()
                val game1 = givenGameExists(path = path)
                val library2 = givenLibraryExists()
                val game2 = givenGameExists(library = library2, path = path)
                val updatedGame1 = game1.copy(metaData = randomMetaData(library = library2, path = path))

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateGame(updatedGame1)
                }

                persistenceService.fetchAllGames() shouldBe listOf(game1, game2)
            }

            "throw an exception when trying to update a game to a non-existing library".inLazyScope({ GameScope() }) {
                val game = givenGameExists()
                val nonExistingLibrary = Library(2, "".toFile(), LibraryData(Platform.pc, ""))

                shouldThrow<JdbcSQLException> {
                    persistenceService.updateGame(game.copy(metaData = randomMetaData(library = nonExistingLibrary)))
                }
            }
        }

        "Game persistence delete" should {
            "delete existing games".inLazyScope({ GameScope() }) {
                val game1 = givenGameExists()
                val game2 = givenGameExists()

                persistenceService.deleteGame(game1.id)
                persistenceService.fetchAllGames() shouldBe listOf(game2)

                persistenceService.deleteGame(game2.id)
                persistenceService.fetchAllGames() shouldBe emptyList<Game>()
            }

            "throw an exception when trying to delete a non-existing game".inLazyScope({ GameScope() }) {
                val game = givenGameExists()

                shouldThrow<IllegalArgumentException> {
                    persistenceService.deleteGame(game.id + 1)
                }
            }

            @Suppress("UNUSED_VARIABLE")
            "delete all games belonging to a library when that library is deleted".inLazyScope({ GameScope() }) {
                val game1 = givenGameExists(library = library)
                val game2 = givenGameExists(library = library)

                val library2 = givenLibraryExists()
                val game3 = givenGameExists(library = library2)
                val game4 = givenGameExists(library = library2)

                persistenceService.deleteLibrary(library.id)
                persistenceService.fetchAllGames() shouldBe listOf(game3, game4)

                persistenceService.deleteLibrary(library2.id)
                persistenceService.fetchAllGames() shouldBe emptyList<Game>()
            }
        }
    }
}