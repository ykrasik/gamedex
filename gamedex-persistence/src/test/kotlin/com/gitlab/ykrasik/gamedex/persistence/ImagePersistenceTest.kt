package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.test.randomUrl
import com.gitlab.ykrasik.gamedex.util.FileSize
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
        "Insert" should {
            "insert and retrieve images inserted for the same game" test {
                val url1 = randomUrl()
                val url2 = randomUrl()

                val image1 = insertImage(game = game, url = url1)
                val image2 = insertImage(game = game, url = url2)

                fetchImage(url1) shouldBe image1
                fetchImage(url2) shouldBe image2
            }

            "insert and retrieve images inserted for different games" test {
                val game2 = givenGame()
                val url1 = randomUrl()
                val url2 = randomUrl()

                val image1 = insertImage(game = game, url = url1)
                val image2 = insertImage(game = game2, url = url2)

                fetchImage(url1) shouldBe image1
                fetchImage(url2) shouldBe image2
            }

            "return null when retrieving a non-existing image" test {
                fetchImage(url = randomUrl()) shouldBe null
            }

            "throw an exception when trying to insert an image with a url of another image belonging to the same game" test {
                val url = randomUrl()
                givenImage(url = url)

                shouldThrow<JdbcSQLException> {
                    insertImage(url = url)
                }
            }

            "throw an exception when trying to insert an image with a url of another image belonging to a different game" test {
                val url = randomUrl()
                givenImage(url = url)

                val game2 = givenGame()

                shouldThrow<JdbcSQLException> {
                    insertImage(game = game2, url = url)
                }
            }
        }

        "Delete" should {
            @Suppress("UNUSED_VARIABLE")
            "automatically delete a game's images when the game is deleted" test {
                val url1 = randomUrl()
                val url2 = randomUrl()
                val url3 = randomUrl()
                val game2 = givenGame()

                val image1 = givenImage(game = game, url = url1)
                val image2 = givenImage(game = game, url = url2)
                val image3 = givenImage(game = game2, url = url3)

                persistenceService.deleteGame(game.id)

                fetchImage(url1) shouldBe null
                fetchImage(url2) shouldBe null
                fetchImage(url3) shouldBe image3
            }

            "automatically delete a game's images when the library the game belongs to is deleted" test {
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2)
                val url = randomUrl()

                givenImage(game = game2, url = url)

                persistenceService.deleteLibrary(library2.id)

                fetchImage(url) shouldBe null
            }
        }

        "FetchExcept" should {
            "return all images & their sizes, except the given urls" test {
                fun expected(vararg results: Pair<String, List<Byte>>) = results.map { it.first to FileSize(it.second.toByteArray().size.toLong()) }

                val url1 = randomUrl()
                val url2 = randomUrl()
                val url3 = randomUrl()
                val game2 = givenGame()

                val image1 = givenImage(game = game, url = url1)
                val image2 = givenImage(game = game, url = url2)
                val image3 = givenImage(game = game2, url = url3)

                persistenceService.fetchImagesExcept(emptyList()) shouldBe expected(url1 to image1, url2 to image2, url3 to image3)
                persistenceService.fetchImagesExcept(listOf(randomUrl())) shouldBe expected(url1 to image1, url2 to image2, url3 to image3)
                persistenceService.fetchImagesExcept(listOf(url1)) shouldBe expected(url2 to image2, url3 to image3)
                persistenceService.fetchImagesExcept(listOf(url1, randomUrl())) shouldBe expected(url2 to image2, url3 to image3)
                persistenceService.fetchImagesExcept(listOf(url1, url2)) shouldBe expected(url3 to image3)
                persistenceService.fetchImagesExcept(listOf(url2, url1)) shouldBe expected(url3 to image3)
                persistenceService.fetchImagesExcept(listOf(url1, url3)) shouldBe expected(url2 to image2)
                persistenceService.fetchImagesExcept(listOf(url3, url1)) shouldBe expected(url2 to image2)
                persistenceService.fetchImagesExcept(listOf(url1, url2, url3)) shouldBe emptyList<String>()
            }
        }
        
        "BatchDelete" should {
            "batch delete images by url" test {
                val url1 = randomUrl()
                val url2 = randomUrl()
                val url3 = randomUrl()
                val game2 = givenGame()

                val image1 = givenImage(game = game, url = url1)
                val image2 = givenImage(game = game, url = url2)
                val image3 = givenImage(game = game2, url = url3)

                persistenceService.deleteImages(emptyList()) shouldBe 0
                fetchImage(url1) shouldBe image1
                fetchImage(url2) shouldBe image2
                fetchImage(url3) shouldBe image3

                persistenceService.deleteImages(listOf(url1, url3, randomUrl())) shouldBe 2
                fetchImage(url1) shouldBe null
                fetchImage(url2) shouldBe image2
                fetchImage(url3) shouldBe null

                persistenceService.deleteImages(listOf(url2)) shouldBe 1
                fetchImage(url2) shouldBe null

                persistenceService.deleteImages(listOf(url1)) shouldBe 0
            }

            @Suppress("UNUSED_VARIABLE")
            "automatically delete a game's images when the game is deleted" test {
                val url1 = randomUrl()
                val url2 = randomUrl()
                val url3 = randomUrl()
                val game2 = givenGame()

                val image1 = givenImage(game = game, url = url1)
                val image2 = givenImage(game = game, url = url2)
                val image3 = givenImage(game = game2, url = url3)

                persistenceService.deleteGames(listOf(game.id))

                fetchImage(url1) shouldBe null
                fetchImage(url2) shouldBe null
                fetchImage(url3) shouldBe image3
            }

            "automatically delete a game's images when the library the game belongs to is deleted" test {
                val library2 = givenLibrary()
                val game2 = givenGame(library = library2)
                val url = randomUrl()

                givenImage(game = game2, url = url)

                persistenceService.deleteLibraries(listOf(library2.id))

                fetchImage(url) shouldBe null
            }
        }
    }

    private infix fun String.test(test: ImageScope.() -> Unit) = inScope(::ImageScope, test)
}