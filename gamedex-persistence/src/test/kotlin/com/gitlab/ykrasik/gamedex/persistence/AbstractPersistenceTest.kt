package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.toFile
import io.kotlintest.TestCaseContext

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:48
 */
abstract class AbstractPersistenceTest : ScopedWordSpec() {
    val persistenceService = PersistenceServiceImpl(PersistenceConfig(
        dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
        user = "sa",
        password = ""
    ))

    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        persistenceService.dropDb()
        test()
    }

    inner open class LibraryScope {
        fun libraryData(platform: Platform = randomEnum<Platform>(), name: String = randomName()) = LibraryData(
            platform = platform,
            name = name
        )

        fun givenLibraryExists(path: String = randomPath(),
                               platform: Platform = randomEnum<Platform>(),
                               name: String = randomName()): Library = insertLibrary(path, platform, name)

        fun insertLibrary(path: String = randomPath(),
                          platform: Platform = randomEnum<Platform>(),
                          name: String = randomName()): Library =
            persistenceService.insertLibrary(path.toFile(), libraryData(platform, name))
    }

    inner open class GameScope : LibraryScope() {
        val library = givenLibraryExists()

        fun randomMetadata(library: Library = this.library, path: String = randomPath()) = Metadata(
            libraryId = library.id,
            path = path,
            updateDate = randomDateTime()
        )

        fun randomProviderData() = ProviderData(
            header = ProviderHeader(
                id = randomString(),
                apiUrl = randomUrl(),
                updateDate = randomDateTime()
            ),
            gameData = GameData(
                siteUrl = randomUrl(),
                name = randomName(),
                description = randomSentence(),
                releaseDate = randomLocalDateString(),
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

        fun randomUserData() = UserData(
            overrides = randomProviderOverrides(),
            tags = listOf(randomString(), randomString()),
            excludedProviders = listOf(randomString(), randomString())
        )

        fun randomProviderOverrides() = mapOf(
            GameDataType.name_ to providerOverride(),
            GameDataType.description to providerOverride(),
            GameDataType.releaseDate to providerOverride(),
            GameDataType.criticScore to providerOverride(),
            GameDataType.userScore to providerOverride(),
            GameDataType.genres to providerOverride(),
            GameDataType.thumbnail to providerOverride(),
            GameDataType.poster to providerOverride(),
            GameDataType.screenshots to providerOverride()
        )

        fun randomCustomOverrides() = mapOf(
            GameDataType.name_ to customDataOverride(randomName()),
            GameDataType.description to customDataOverride(randomSentence()),
            GameDataType.releaseDate to customDataOverride(randomLocalDateString()),
            GameDataType.criticScore to customDataOverride(randomScore().score),
            GameDataType.userScore to customDataOverride(randomScore().score),
            GameDataType.genres to customDataOverride(listOf(randomString(), randomString(), randomString())),
            GameDataType.thumbnail to customDataOverride(randomUrl()),
            GameDataType.poster to customDataOverride(randomUrl()),
            GameDataType.screenshots to customDataOverride(listOf(randomUrl(), randomUrl()))
        )

        private fun providerOverride() = GameDataOverride.Provider(randomString())
        private fun customDataOverride(data: Any) = GameDataOverride.Custom(data)

        fun givenGameExists(library: Library = this.library, path: String = randomPath()): RawGame = insertGame(library, path)

        fun insertGame(library: Library = this.library, path: String = randomPath()): RawGame =
            insertGame(metadata = randomMetadata(library, path))

        fun insertGame(metadata: Metadata = randomMetadata(),
                       providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
                       userData: UserData? = randomUserData()): RawGame =
            persistenceService.insertGame(metadata, providerData, userData)
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