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

        fun randomRawGameData() = RawGameData(
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

        fun providerOverride(provider: GameProviderType = randomEnum()) = GameDataOverride.Provider(provider)
        fun customDataOverride(data: Any) = GameDataOverride.Custom(data)

        fun givenGameExists(library: Library = this.library, path: String = randomPath()): RawGame = insertGame(library, path)
        fun insertGame(library: Library = this.library, path: String = randomPath()): RawGame =
            persistenceService.insertGame(
                metaData = randomMetaData(library, path),
                rawGameData = listOf(randomRawGameData(), randomRawGameData())
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