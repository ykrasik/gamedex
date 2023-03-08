/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.util.file

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 13:48
 */
abstract class AbstractPersistenceTest<Scope> : Spec<Scope>() {
    override fun beforeEach() = persistenceService.dropDb()

    open class LibraryScope {
        fun libraryData(
            name: String = randomName(),
            path: String = randomPath(),
            type: LibraryType = LibraryType.Digital,
            platform: Platform = randomEnum(),
        ) = LibraryData(name, path.file, type, platform)

        fun givenLibrary(
            path: String = randomPath(),
            platform: Platform = randomEnum(),
            name: String = randomName(),
            type: LibraryType = LibraryType.Digital,
        ): Library = insertLibrary(path, platform, name, type)

        fun insertLibrary(
            path: String = randomPath(),
            platform: Platform = randomEnum(),
            name: String = randomName(),
            type: LibraryType = LibraryType.Digital,
        ): Library = persistenceService.insertLibrary(libraryData(name, path, type, platform))

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
                providerId = randomWord(),
                providerGameId = randomUrl()
            ),
            gameData = GameData(
                name = randomName(),
                description = randomParagraph(),
                releaseDate = randomLocalDateString(),
                criticScore = randomScore(),
                userScore = randomScore(),
                genres = listOf(randomWord(), randomWord()),
                thumbnailUrl = randomUrl(),
                posterUrl = randomUrl(),
                screenshotUrls = listOf(randomUrl(), randomUrl())
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
            GameDataType.Name to providerOverride(),
            GameDataType.Description to providerOverride(),
            GameDataType.ReleaseDate to providerOverride(),
            GameDataType.CriticScore to providerOverride(),
            GameDataType.UserScore to providerOverride(),
            GameDataType.Genres to providerOverride(),
            GameDataType.Thumbnail to providerOverride(),
            GameDataType.Poster to providerOverride(),
            GameDataType.Screenshots to providerOverride()
        )

        fun randomCustomOverrides() = mapOf(
            GameDataType.Name to customDataOverride(randomName()),
            GameDataType.Description to customDataOverride(randomParagraph()),
            GameDataType.ReleaseDate to customDataOverride(randomLocalDateString()),
            GameDataType.CriticScore to customDataOverride(randomScore().score),
            GameDataType.UserScore to customDataOverride(randomScore().score),
            GameDataType.Genres to customDataOverride(listOf(randomWord(), randomWord(), randomWord())),
            GameDataType.Thumbnail to customDataOverride(randomUrl()),
            GameDataType.Poster to customDataOverride(randomUrl()),
            GameDataType.Screenshots to customDataOverride(listOf(randomUrl(), randomUrl()))
        )

        private fun providerOverride() = GameDataOverride.Provider(randomWord())
        private fun customDataOverride(data: Any) = GameDataOverride.Custom(data)

        fun givenGame(
            library: Library = this.library,
            path: String = randomPath(),
            providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
            userData: UserData = randomUserData(),
        ): RawGame = insertGame(library, path, providerData, userData)

        fun insertGame(
            library: Library = this.library,
            path: String = randomPath(),
            providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
            userData: UserData = randomUserData(),
        ): RawGame = insertGame(randomMetadata(library, path), providerData, userData)

        fun insertGame(
            metadata: Metadata = randomMetadata(),
            providerData: List<ProviderData> = listOf(randomProviderData(), randomProviderData()),
            userData: UserData = randomUserData(),
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