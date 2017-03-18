package com.gitlab.ykrasik.gamedex.persistence.dao

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.persistence.*
import io.kotlintest.specs.StringSpec
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 20:55
 */
abstract class PersistenceTest : StringSpec() {
    val initializer = DbInitializer(PersistenceConfig(
        dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    ))
    val persistenceService = PersistenceServiceImpl(initializer)

    private var imageId = 1

    override fun beforeEach() {
        initializer.reload()
        imageId = 1
    }

    val now = DateTime.now()

    val imageUrls = ImageUrls(
        thumbnailUrl = "someThumbnailUrl",
        posterUrl = "somePosterUrl",
        screenshotUrls = listOf("someScreenshotUrl1", "someScreenshotUrl2")
    )

    val providerData = ProviderData(
        type = DataProviderType.GiantBomb,
        apiUrl = "someDetailUrl",
        url = "someUrl"
    )

    val gameData = GameData(
        name = "someName",
        description = "someDescription",
        releaseDate = now.toLocalDate(),
        criticScore = 1.2,
        userScore = 3.4,
        genres = listOf("someGenre1", "someGenre2")
    )

    fun givenLibraryExists(id: Int, path: String = id.toString(), platform: GamePlatform = GamePlatform.pc, name: String = path): Library {
        val file = path.toFile()
        val data = LibraryData(platform, name)
        val library = persistenceService.insert(AddLibraryRequest(file, data))
        library shouldBe Library(id, file, data)
        return library
    }

    fun givenGameExists(id: Int,
                        library: Library,
                        path: String = id.toString(),
                        imageUrls: ImageUrls = this.imageUrls): Game {
        val metaData = MetaData(library.id, path.toFile(), lastModified = now)
        val providerData = listOf(this.providerData)
        val game = persistenceService.insert(AddGameRequest(
            metaData = metaData,
            gameData = gameData,
            providerData = providerData,
            imageUrls = imageUrls
        ))

        val imageIds = expectedImageIds(imageUrls)

        game shouldBe Game(id, metaData, gameData, providerData, imageIds)
        return game
    }

    private fun expectedImageIds(imageUrls: ImageUrls): ImageIds {
        val thumbnailId = imageUrls.thumbnailUrl?.run { nextImageId }
        val posterId = imageUrls.posterUrl?.run { nextImageId }
        val screenshotIds = imageUrls.screenshotUrls.mapNotNull { nextImageId }
        return ImageIds(thumbnailId, posterId, screenshotIds)
    }

    private val nextImageId: Int get() {
        val id = imageId
        imageId += 1
        return id
    }
}