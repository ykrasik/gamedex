/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.app.api.settings.minOrder
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.genre.GenreService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.settings.GameSettingsRepository
import com.gitlab.ykrasik.gamedex.core.settings.ProviderOrderSettingsRepository
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.firstNotNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 20/04/2017
 * Time: 20:35
 */
@Singleton
class GameFactory @Inject constructor(
    private val libraryService: LibraryService,
    private val fileSystemService: FileSystemService,
    private val gameSettingsRepository: GameSettingsRepository,
    private val providerOrderSettingsRepo: ProviderOrderSettingsRepository,
    private val genreService: GenreService,
    private val filterService: FilterService
) {
    fun create(rawGame: RawGame): Game {
        val library = libraryService[rawGame.metadata.libraryId]
        val gameData = rawGame.toGameData()
        val folderName = fileSystemService.analyzeFolderName(rawGame.metadata.path.file.name)
        val genres = gameData.genres.map(genreService::get)
        val fileTree = fileSystemService.fileTree(rawGame.id, library.path.resolve(rawGame.metadata.path))

        val game = Game(
            library = library,
            rawGame = rawGame,
            gameData = gameData,
            folderName = folderName,
            fileTree = fileTree,
            genres = genres,
            filterTags = emptyList()
        )

        val filterTags = filterService.calcFilterTags(game)
        return if (filterTags.isNotEmpty()) {
            game.copy(filterTags = filterTags)
        } else {
            game
        }
    }

    private fun RawGame.toGameData(): GameData {
        val criticScoreOrder = providerData.sortedByDescending { it.gameData.criticScore?.numReviews ?: -1 }.map { it.providerId }
        val userScoreOrder = providerData.sortedByDescending { it.gameData.userScore?.numReviews ?: -1 }.map { it.providerId }

        val thumbnailUrl = firstBy(providerOrderSettingsRepo.thumbnail, userData.thumbnailOverride()) { it.gameData.thumbnailUrl }
        val posterUrl = firstBy(providerOrderSettingsRepo.poster, userData.posterOverride()) { it.gameData.posterUrl }
        val screenshotUrls = listBy(providerOrderSettingsRepo.screenshot, userData.screenshotsOverride()) { it.gameData.screenshotUrls }.take(gameSettingsRepository.maxScreenshots)

        return GameData(
            name = firstBy(providerOrderSettingsRepo.name, userData.nameOverride()) { it.gameData.name } ?: metadata.path.file.name,
            description = firstBy(providerOrderSettingsRepo.description, userData.descriptionOverride()) { it.gameData.description },
            releaseDate = firstBy(providerOrderSettingsRepo.releaseDate, userData.releaseDateOverride()) { it.gameData.releaseDate },
            criticScore = firstBy(criticScoreOrder, userData.criticScoreOverride(), converter = ::asCustomScore) {
                it.gameData.criticScore.minOrNull()
            },
            userScore = firstBy(userScoreOrder, userData.userScoreOverride(), converter = ::asCustomScore) {
                it.gameData.userScore.minOrNull()
            },
            genres = genreService.processGenres(unsortedListBy(userData.genresOverride()) { it.gameData.genres }),
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun asCustomScore(value: Any) = Score(value as Double, numReviews = 0)
    private fun Score?.minOrNull() = this?.let { if (it.numReviews >= 4) it else null }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T> RawGame.firstBy(
        defaultOrder: Order,
        override: GameDataOverride?,
        converter: (Any) -> T = { it as T },
        crossinline extractor: (ProviderData) -> T?
    ): T? = when (override) {
        is GameDataOverride.Custom -> converter(override.value)
        else -> {
            val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
            sorted.findFirst(extractor)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T> RawGame.listBy(defaultOrder: Order, override: GameDataOverride?, extractor: (ProviderData) -> List<T>): List<T> =
        when (override) {
            is GameDataOverride.Custom -> override.value as List<T>
            else -> {
                val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
                sorted.flatMap(extractor)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private inline fun <T> RawGame.unsortedListBy(override: GameDataOverride?, extractor: (ProviderData) -> List<T>): List<T> =
        when (override) {
            is GameDataOverride.Custom -> override.value as List<T>
            else -> providerData.flatMap(extractor)
        }

    private fun RawGame.sortDataBy(order: Order, override: GameDataOverride.Provider?): List<ProviderData> =
        providerData.sortedBy {
            val providerId = it.providerId
            if (providerId == override?.provider) {
                minOrder
            } else {
                order.indexOf(providerId).let {
                    if (it == -1) 99999 else it
                }
            }
        }

    private inline fun <T> List<ProviderData>.findFirst(crossinline extractor: (ProviderData) -> T?): T? =
        this.asSequence().map { extractor(it) }.firstNotNull()
}