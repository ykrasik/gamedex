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

package com.gitlab.ykrasik.gamedex.core.game

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.app.api.settings.Order
import com.gitlab.ykrasik.gamedex.app.api.settings.minOrder
import com.gitlab.ykrasik.gamedex.core.file.FileSystemService
import com.gitlab.ykrasik.gamedex.core.library.LibraryService
import com.gitlab.ykrasik.gamedex.core.report.ReportService
import com.gitlab.ykrasik.gamedex.core.settings.SettingsService
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
    private val config: GameConfig,
    private val libraryService: LibraryService,
    private val fileSystemService: FileSystemService,
    private val settingsService: SettingsService,
    private val reportService: ReportService
) {
    fun create(rawGame: RawGame): Game {
        val library = libraryService[rawGame.metadata.libraryId]
        val gameData = rawGame.toGameData()
        val folderName = fileSystemService.analyzeFolderName(rawGame.metadata.path.file.name)
        val fileTree = fileSystemService.fileTree(rawGame.id, library.path.resolve(rawGame.metadata.path))

        val game = Game(
            library = library,
            rawGame = rawGame,
            gameData = gameData,
            folderName = folderName,
            fileTree = fileTree,
            reportTags = emptyList()
        )

        val reportTags = reportService.calcReportTags(game)
        return if (reportTags.isNotEmpty()) {
            game.copy(reportTags = reportTags)
        } else {
            game
        }
    }

    private fun RawGame.toGameData(): GameData = GameData(
        name = firstBy(settingsService.providerOrder.name, userData.nameOverride()) { it.gameData.name } ?: metadata.path.file.name,
        description = firstBy(settingsService.providerOrder.description, userData.descriptionOverride()) { it.gameData.description },
        releaseDate = firstBy(settingsService.providerOrder.releaseDate, userData.releaseDateOverride()) { it.gameData.releaseDate },
        // TODO: Choose score with most votes.
        criticScore = firstBy(settingsService.providerOrder.criticScore, userData.criticScoreOverride(), converter = ::asCustomScore) {
            it.gameData.criticScore.minOrNull()
        },
        userScore = firstBy(settingsService.providerOrder.userScore, userData.userScoreOverride(), converter = ::asCustomScore) {
            it.gameData.userScore.minOrNull()
        },
        genres = unsortedListBy(userData.genresOverride()) { it.gameData.genres }.flatMap(config::mapGenre).distinct().take(config.maxGenres),
        imageUrls = toImageUrls()
    )

    private fun asCustomScore(value: Any) = Score(value as Double, numReviews = 0)
    private fun Score?.minOrNull() = this?.let { if (it.numReviews >= 4) it else null }

    private fun RawGame.toImageUrls(): ImageUrls {
        val thumbnailUrl = firstBy(settingsService.providerOrder.thumbnail, userData.thumbnailOverride()) { it.gameData.imageUrls.thumbnailUrl }
        val posterUrl = firstBy(settingsService.providerOrder.poster, userData.posterOverride()) { it.gameData.imageUrls.posterUrl }
        val screenshotUrls = listBy(settingsService.providerOrder.screenshot, userData.screenshotsOverride()) { it.gameData.imageUrls.screenshotUrls }.take(config.maxScreenshots)
        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

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
            val providerId = it.header.id
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