/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.*
import com.gitlab.ykrasik.gamedex.util.toFile
import io.kotlintest.TestCaseContext

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:48
 */
abstract class AbstractPersistenceTest<Scope> : ScopedWordSpec<Scope>() {
    override fun interceptTestCase(context: TestCaseContext, test: () -> Unit) {
        persistenceService.dropDb()
        test()
    }

    open class LibraryScope {
        fun libraryData(
            name: String = randomName(),
            path: String = randomPath(),
            platform: Platform = randomEnum()
        ) = LibraryData(name, path.toFile(), platform)

        fun givenLibrary(
            path: String = randomPath(),
            platform: Platform = randomEnum(),
            name: String = randomName()
        ): Library = insertLibrary(path, platform, name)

        fun insertLibrary(
            path: String = randomPath(),
            platform: Platform = randomEnum(),
            name: String = randomName()
        ): Library = persistenceService.insertLibrary(libraryData(name, path, platform))

        fun fetchLibraries() = persistenceService.fetchLibraries()
    }

    open class GameScope : LibraryScope() {
        val library = givenLibrary()

        fun randomMetadata(library: Library = this.library, path: String = randomPath()) = Metadata(
            libraryId = library.id,
            path = path,
            timestamp = randomTimestamp()
        )

        fun randomProviderData() = ProviderData(
            header = ProviderHeader(
                id = randomWord(),
                apiUrl = randomUrl()
            ),
            gameData = GameData(
                name = randomName(),
                description = randomParagraph(),
                releaseDate = randomLocalDateString(),
                criticScore = randomScore(),
                userScore = randomScore(),
                genres = listOf(randomWord(), randomWord()),
                imageUrls = ImageUrls(
                    thumbnailUrl = randomUrl(),
                    posterUrl = randomUrl(),
                    screenshotUrls = listOf(randomUrl(), randomUrl())
                )
            ),
            siteUrl = randomUrl(),
            timestamp = randomTimestamp()
        )

        fun randomUserData() = UserData(
            overrides = randomProviderOverrides(),
            tags = listOf(randomWord(), randomWord()),
            excludedProviders = listOf(randomWord(), randomWord())
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
            GameDataType.description to customDataOverride(randomParagraph()),
            GameDataType.releaseDate to customDataOverride(randomLocalDateString()),
            GameDataType.criticScore to customDataOverride(randomScore().score),
            GameDataType.userScore to customDataOverride(randomScore().score),
            GameDataType.genres to customDataOverride(listOf(randomWord(), randomWord(), randomWord())),
            GameDataType.thumbnail to customDataOverride(randomUrl()),
            GameDataType.poster to customDataOverride(randomUrl()),
            GameDataType.screenshots to customDataOverride(listOf(randomUrl(), randomUrl()))
        )

        private fun providerOverride() = GameDataOverride.Provider(randomWord())
        private fun customDataOverride(data: Any) = GameDataOverride.Custom(data)

        fun givenGame(
            library: Library = this.library,
            path: String = randomPath(),
            providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
            userData: UserData = randomUserData()
        ): RawGame = insertGame(library, path, providerData, userData)

        fun insertGame(
            library: Library = this.library,
            path: String = randomPath(),
            providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
            userData: UserData = randomUserData()
        ): RawGame = insertGame(randomMetadata(library, path), providerData, userData)

        fun insertGame(
            metadata: Metadata = randomMetadata(),
            providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
            userData: UserData = randomUserData()
        ): RawGame = persistenceService.insertGame(metadata, providerData, userData)

        fun fetchGames() = persistenceService.fetchGames()
    }

    companion object {
        val persistenceService = PersistenceServiceImpl(
            PersistenceConfig(
                dbUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )
        )
    }
}