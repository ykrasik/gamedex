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
        fun randomLibraryDate() = LibraryData(
            platform = randomEnum<Platform>(),
            name = randomName()
        )

        fun givenLibraryExists(path: String = randomPath()): Library = insertLibrary(path)
        fun insertLibrary(path: String = randomPath()): Library =
            persistenceService.insertLibrary(path.toFile(), randomLibraryDate())
    }

    inner open class GameScope : LibraryScope() {
        val library = givenLibraryExists()

        fun randomMetaData(library: Library = this.library, path: String = randomPath()) = MetaData(
            libraryId = library.id,
            path = path.toFile(),
            lastModified = randomDateTime()
        )

        fun randomProviderData() = ProviderData(
            header = ProviderHeader(
                type = randomEnum<GameProviderType>(),
                apiUrl = randomUrl(),
                siteUrl = randomUrl()
            ),
            gameData = GameData(
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

        fun providerOverrides() = mapOf(
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

        fun customOverrides() = mapOf(
            GameDataType.name_ to customDataOverride(randomName()),
            GameDataType.description to customDataOverride(randomSentence()),
            GameDataType.releaseDate to customDataOverride(randomLocalDateString()),
            GameDataType.criticScore to customDataOverride(randomScore()),
            GameDataType.userScore to customDataOverride(randomScore()),
            GameDataType.genres to customDataOverride(listOf(randomString(), randomString(), randomString())),
            GameDataType.thumbnail to customDataOverride(randomUrl()),
            GameDataType.poster to customDataOverride(randomUrl()),
            GameDataType.screenshots to customDataOverride(listOf(randomUrl(), randomUrl()))
        )

        private fun providerOverride(provider: GameProviderType = randomEnum()) = GameDataOverride.Provider(provider)
        private fun customDataOverride(data: Any) = GameDataOverride.Custom(data)

        fun givenGameExists(library: Library = this.library, path: String = randomPath()): RawGame = insertGame(library, path)
        fun insertGame(library: Library = this.library, path: String = randomPath()): RawGame =
            persistenceService.insertGame(
                metaData = randomMetaData(library, path),
                providerData = listOf(randomProviderData(), randomProviderData()),
                userData = UserData(overrides = providerOverrides())
            )
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