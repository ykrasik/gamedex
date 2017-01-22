package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.toFile
import com.github.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.persistence.*
import io.kotlintest.specs.StringSpec
import org.joda.time.DateTime

/**
 * User: ykrasik
 * Date: 06/10/2016
 * Time: 20:55
 */
abstract class PersistenceTest : StringSpec() {
    val persistenceService = PersistenceServiceImpl(TestDbInitializer.initializer)

    override fun beforeEach() {
        TestDbInitializer.reload()
    }

    val now = DateTime.now()

    val imageData = GameImageData(
        thumbnailUrl = "someThumbnailUrl",
        posterUrl = "somePosterUrl",
        screenshot1Url = "someScreenshot1Url",
        screenshot2Url = "someScreenshot2Url",
        screenshot3Url = "someScreenshot3Url",
        screenshot4Url = "someScreenshot4Url",
        screenshot5Url = "someScreenshot5Url",
        screenshot6Url = "someScreenshot6Url",
        screenshot7Url = "someScreenshot7Url",
        screenshot8Url = "someScreenshot8Url",
        screenshot9Url = "someScreenshot9Url",
        screenshot10Url = "someScreenshot10Url"
    )

    val providerData = ProviderGameData(
        type = DataProviderType.GiantBomb,
        detailUrl = "someDetailUrl",
        data = GameData(
            name = "someName",
            description = "someDescription",
            releaseDate = now.toLocalDate(),
            criticScore = 1.2,
            userScore = 3.4,
            genres = listOf("someGenre1", "someGenre2")
        ),
        imageData = imageData
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
                        providerData: List<ProviderGameData> = listOf(this.providerData)): RawGame {
        val metaData = GameMetaData(library.id, path.toFile(), lastModified = now)
        val game = persistenceService.insert(AddGameRequest(
            metaData = metaData,
            providerData = providerData,
            imageData = imageData
        ))

        game shouldBe RawGame(id = id, metaData = metaData, providerData = providerData)
        return game
    }
}