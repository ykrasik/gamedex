package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.toFile
import io.kotlintest.TestCaseContext
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 15:41
 */
class PersistenceServiceIT : ScopedWordSpec() {
    val initializer = DbInitializer(PersistenceConfig(
        dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    ))
    val persistenceService = PersistenceServiceImpl(initializer)

    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        initializer.reload()
        test()
    }

    init {
        "Library persistence" should {
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

        "Game persistence" should {
            "insert and retrieve games".inLazyScope({ GameScope() }) {
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

            "throw an exception when trying to insert a game for an already existing path in a different library".inLazyScope({ GameScope() }) {
                val path = randomPath()
                givenGameExists(path = path)

                val library2 = givenLibraryExists()

                shouldThrow<JdbcSQLException> {
                    insertGame(library = library2, path = path)
                }
            }

            "throw an exception when trying to insert a game for a non-existing library".inLazyScope({ GameScope() }) {
                val nonExistingLibrary = Library(2, "".toFile(), LibraryData(GamePlatform.pc, ""))

                shouldThrow<JdbcSQLException> {
                    insertGame(library = nonExistingLibrary)
                }
            }

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

        "Image persistence" should {
            "insert and retrieve images inserted for the same game".inLazyScope({ ImageScope() }) {
                val url1 = randomUrl()
                val url2 = randomUrl()

                val image1 = insertImage(game = game, url = url1)
                val image2 = insertImage(game = game, url = url2)

                fetchImage(url1) shouldBe image1
                fetchImage(url2) shouldBe image2
            }

            "insert and retrieve images inserted for different games".inLazyScope({ ImageScope() }) {
                val game2 = givenGameExists()
                val url1 = randomUrl()
                val url2 = randomUrl()

                val image1 = insertImage(game = game, url = url1)
                val image2 = insertImage(game = game2, url = url2)

                fetchImage(url1) shouldBe image1
                fetchImage(url2) shouldBe image2
            }

            "return null when retrieving a non-existing image".inLazyScope({ ImageScope() }) {
                fetchImage(url = randomUrl()) shouldBe null
            }

            "throw an exception when trying to insert an image with a url of another image belonging to the same game".inLazyScope({ ImageScope() }) {
                val url = randomUrl()
                givenImageExists(url = url)

                shouldThrow<JdbcSQLException> {
                    insertImage(url = url)
                }
            }

            "throw an exception when trying to insert an image with a url of another image belonging to a different game".inLazyScope({ ImageScope() }) {
                val url = randomUrl()
                givenImageExists(url = url)

                val game2 = givenGameExists()

                shouldThrow<JdbcSQLException> {
                    insertImage(game = game2, url = url)
                }
            }

            @Suppress("UNUSED_VARIABLE")
            "delete a game's images when the game is deleted".inLazyScope({ ImageScope() }) {
                val url1 = randomUrl()
                val url2 = randomUrl()
                val url3 = randomUrl()
                val game2 = givenGameExists()

                val image1 = givenImageExists(game = game, url = url1)
                val image2 = givenImageExists(game = game, url = url2)
                val image3 = givenImageExists(game = game2, url = url3)

                persistenceService.deleteGame(game.id)

                fetchImage(url1) shouldBe null
                fetchImage(url2) shouldBe null
                fetchImage(url3) shouldBe image3
            }

            "delete a game's images when the library the game belongs to is deleted".inLazyScope({ ImageScope() }) {
                val library2 = givenLibraryExists()
                val game2 = givenGameExists(library = library2)
                val url = randomUrl()

                givenImageExists(game = game2, url = url)

                persistenceService.deleteLibrary(library2.id)

                fetchImage(url) shouldBe null
            }
        }
    }

    inner open class LibraryScope {
        fun givenLibraryExists(path: String = randomPath()): Library = insertLibrary(path)
        fun insertLibrary(path: String = randomPath()): Library {
            val file = path.toFile()
            val data = LibraryData(platform = randomEnum<GamePlatform>(), name = randomName())
            val library = persistenceService.insertLibrary(file, data)
            library.path shouldBe file
            library.data shouldBe data
            return library
        }
    }

    inner open class GameScope : LibraryScope() {
        val library = givenLibraryExists()

        private fun randomRawGameData() = RawGameData(
            providerData = ProviderData(
                type = randomEnum<GameProviderType>(),
                apiUrl = randomUrl(),
                siteUrl = randomUrl()
            ),
            gameData = GameData(
                name = randomName(),
                description = randomSentence(),
                releaseDate = randomLocalDate(),
                criticScore = randomScore(),
                userScore = randomScore(),
                genres = listOf(randomString(), randomString())
            ),
            imageUrls = ImageUrls(
                thumbnailUrl = randomUrl(),
                posterUrl = randomUrl(),
                screenshotUrls = listOf(randomUrl(), randomUrl())
            )
        )

        fun givenGameExists(library: Library = this.library, path: String = randomPath()): RawGame = insertGame(library, path)
        fun insertGame(library: Library = this.library, path: String = randomPath()): RawGame {
            val metaData = MetaData(library.id, path.toFile(), lastModified = randomDateTime())
            val providerData = listOf(randomRawGameData(), randomRawGameData())
            val game = persistenceService.insertGame(metaData, providerData)
            game.metaData shouldBe metaData
            game.rawGameData shouldBe providerData
            return game
        }
    }

    inner open class ImageScope : GameScope() {
        val game = givenGameExists()

        fun givenImageExists(game: RawGame = this.game, url: String = randomUrl()): List<Byte> = insertImage(game, url)
        fun insertImage(game: RawGame = this.game, url: String = randomUrl()): List<Byte> {
            val data = randomString().toByteArray()
            persistenceService.insertImage(game.id, url, data)
            return data.toList()
        }

        fun fetchImage(url: String) = persistenceService.fetchImage(url)?.toList()
    }
}