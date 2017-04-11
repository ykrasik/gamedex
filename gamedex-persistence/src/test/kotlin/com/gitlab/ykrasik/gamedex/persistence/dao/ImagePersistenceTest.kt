package com.gitlab.ykrasik.gamedex.persistence.dao

import com.gitlab.ykrasik.gamedex.datamodel.Game
import com.gitlab.ykrasik.gamedex.datamodel.GameImage
import com.gitlab.ykrasik.gamedex.datamodel.ImageUrls
import com.gitlab.ykrasik.gamedex.datamodel.Library
import org.h2.jdbc.JdbcSQLException
import java.util.*

/**
 * User: ykrasik
 * Date: 08/10/2016
 * Time: 15:41
 */
class ImagePersistenceTest : PersistenceTest() {
    lateinit var library: Library
    lateinit var game: Game

    val data = "someData".toByteArray()

    override fun beforeEach() {
        super.beforeEach()
        library = givenLibraryExists(1)
    }

    init {
        "Update & fetch thumbnail" {
            game = givenGameExists(1, library)
            verifyImage(1, imageUrls.thumbnailUrl!!, bytes = null)
            updateImage(1, "someUrl", data)
            verifyImage(1, "someUrl", data)
        }

        "Update & fetch poster" {
            game = givenGameExists(1, library)
            verifyImage(2, imageUrls.posterUrl!!, bytes = null)
            updateImage(2, "someUrl", data)
            verifyImage(2, "someUrl", data)
        }

        "Update & fetch screenshots1" {
            game = givenGameExists(1, library)
            verifyImage(3, imageUrls.screenshotUrls.first(), bytes = null)
            updateImage(3, "someUrl", data)
            verifyImage(3, "someUrl", data)
        }

        "Update & fetch screenshot2" {
            game = givenGameExists(1, library)
            verifyImage(4, imageUrls.screenshotUrls[1], bytes = null)
            updateImage(4, "someUrl", data)
            verifyImage(4, "someUrl", data)
        }

        "Insert a game without any images" {
            game = givenGameExists(1, library, imageUrls = ImageUrls(null, null, emptyList()))

            persistenceService.fetchAllGames() shouldBe listOf(game)
        }

        "Insert a game with only a thumbnail" {
            game = givenGameExists(1, library, imageUrls = ImageUrls("thumbnailUrl", null, emptyList()))

            persistenceService.fetchAllGames() shouldBe listOf(game)
        }

        "Insert a game with only a poster" {
            game = givenGameExists(1, library, imageUrls = ImageUrls(null, "posterUrl", emptyList()))

            persistenceService.fetchAllGames() shouldBe listOf(game)
        }

        "Insert a game with only a screenshot" {
            game = givenGameExists(1, library, imageUrls = ImageUrls(null, null, listOf("screenshotUrl")))

            persistenceService.fetchAllGames() shouldBe listOf(game)
        }

        "Throw an exception when trying to insert any image with an already existing url" {
            // TODO: Implement this!
            shouldThrow<JdbcSQLException> {
                givenGameExists(1, library, imageUrls = ImageUrls("someUrl", "someUrl", emptyList()))
            }
        }

        "Delete a game's images when the game is deleted" {
            game = givenGameExists(1, library)
            persistenceService.deleteGame(game.id)

            shouldThrow<NoSuchElementException> {
                fetchImage(1)
            }
            shouldThrow<NoSuchElementException> {
                fetchImage(2)
            }
            shouldThrow<NoSuchElementException> {
                fetchImage(3)
            }
            shouldThrow<NoSuchElementException> {
                fetchImage(4)
            }

            shouldThrow<IllegalStateException> {
                updateImage(1, "someUrl")
            }
            shouldThrow<IllegalStateException> {
                updateImage(2, "someUrl")
            }
            shouldThrow<IllegalStateException> {
                updateImage(3, "someUrl")
            }
            shouldThrow<IllegalStateException> {
                updateImage(4, "someUrl")
            }
        }
    }

    fun fetchImage(id: Int) = persistenceService.fetchImage(id)

    fun verifyImage(id: Int, url: String, bytes: ByteArray? = null) {
        val image = fetchImage(id)
        image shouldBe GameImage(id, url, bytes)
    }

    fun updateImage(id: Int, url: String, bytes: ByteArray? = null) {
        persistenceService.updateImage(GameImage(id, url, bytes))
    }
}