package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.datamodel.*

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
        game = givenGameExists(1, library)
    }

    init {
        "Update & fetch thumbnail" {
            testImageType(GameImageType.Thumbnail)
        }

        "Update & fetch poster" {
            testImageType(GameImageType.Poster)
        }

        "Update & fetch screenshot1" {
            testImageType(GameImageType.Screenshot1)
        }

        "Update & fetch screenshot2" {
            testImageType(GameImageType.Screenshot2)
        }

        "Update & fetch screenshot3" {
            testImageType(GameImageType.Screenshot3)
        }

        "Update & fetch screenshot4" {
            testImageType(GameImageType.Screenshot4)
        }

        "Update & fetch screenshot5" {
            testImageType(GameImageType.Screenshot5)
        }

        "Update & fetch screenshot6" {
            testImageType(GameImageType.Screenshot6)
        }

        "Update & fetch screenshot7" {
            testImageType(GameImageType.Screenshot7)
        }

        "Update & fetch screenshot8" {
            testImageType(GameImageType.Screenshot8)
        }

        "Update & fetch screenshot9" {
            testImageType(GameImageType.Screenshot9)
        }

        "Update & fetch screenshot10" {
            testImageType(GameImageType.Screenshot10)
        }

        "Delete a game's images when the game is deleted" {
            persistenceService.deleteGame(game.id)

            shouldThrow<IllegalArgumentException> {
                fetchImage(GameImageType.Thumbnail)
            }

            shouldThrow<IllegalStateException> {
                updateImage(GameImageType.Thumbnail)
            }
        }
    }

    fun testImageType(type: GameImageType) {
        verifyAllImagesEmpty()
        updateImage(type)
        verifyAllImagesEmpty(except = type)
        verifyImage(type, url = null, bytes = data)
    }

    fun fetchImage(type: GameImageType) = persistenceService.fetchImage(imageId(type))
    fun image(type: GameImageType, url: String? = null, bytes: ByteArray? = null) = GameImage(imageId(type), url, bytes)
    fun imageId(type: GameImageType) = GameImageId(game.id, type)

    fun updateImage(type: GameImageType) =
        persistenceService.updateImage(GameImage(GameImageId(game.id, type), url = null, bytes =data))

    fun verifyAllImagesEmpty(except: GameImageType? = null) {
        GameImageType.values().forEach {
            if (it != except) {
                verifyImage(it, url = it.toUrl(), bytes = null)
            }
        }
    }

    fun verifyImage(type: GameImageType, url: String?, bytes: ByteArray?) {
        fetchImage(type) shouldBe image(type, url, bytes)
    }

    private fun GameImageType.toUrl() = when (this) {
        GameImageType.Thumbnail -> imageData.thumbnailUrl
        GameImageType.Poster -> imageData.posterUrl
        GameImageType.Screenshot1 -> imageData.screenshot1Url
        GameImageType.Screenshot2 -> imageData.screenshot2Url
        GameImageType.Screenshot3 -> imageData.screenshot3Url
        GameImageType.Screenshot4 -> imageData.screenshot4Url
        GameImageType.Screenshot5 -> imageData.screenshot5Url
        GameImageType.Screenshot6 -> imageData.screenshot6Url
        GameImageType.Screenshot7 -> imageData.screenshot7Url
        GameImageType.Screenshot8 -> imageData.screenshot8Url
        GameImageType.Screenshot9 -> imageData.screenshot9Url
        GameImageType.Screenshot10 -> imageData.screenshot10Url
    }
}