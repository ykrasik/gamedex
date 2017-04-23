package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.test.randomUrl
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import org.h2.jdbc.JdbcSQLException

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:51
 */
class ImagePersistenceTest : AbstractPersistenceTest() {
    init {
        "Image persistence insert" should {
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
        }

        "Image persistence delete" should {
            @Suppress("UNUSED_VARIABLE")
            "automatically delete a game's images when the game is deleted".inLazyScope({ ImageScope() }) {
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

            "automatically delete a game's images when the library the game belongs to is deleted".inLazyScope({ ImageScope() }) {
                val library2 = givenLibraryExists()
                val game2 = givenGameExists(library = library2)
                val url = randomUrl()

                givenImageExists(game = game2, url = url)

                persistenceService.deleteLibrary(library2.id)

                fetchImage(url) shouldBe null
            }
        }
    }
}