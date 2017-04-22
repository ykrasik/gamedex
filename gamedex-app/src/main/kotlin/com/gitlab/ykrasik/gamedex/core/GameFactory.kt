package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.firstNotNull

/**
 * User: ykrasik
 * Date: 20/04/2017
 * Time: 20:35
 */
class GameFactory {
    private val maxScreenshots = 10

    fun create(rawGame: RawGame): Game {
        val gameData = rawGame.toGameData()
        val imageUrls = rawGame.toImageUrls()
        val providerData = rawGame.toProviderData()

        return Game(
            id = rawGame.id,
            metaData = rawGame.metaData,
            gameData = gameData,
            providerData = providerData,
            imageUrls = imageUrls
        )
    }

    private fun RawGame.toGameData(): GameData {
        val dataByBasicPriority = this.providerDataBy { it.basicDataPriority }
        val dataByScorePriority = this.providerDataBy { it.scorePriority }

        return GameData(
            name = dataByBasicPriority.findFirst { it.gameData.name } ?: metaData.path.name,
            description = dataByBasicPriority.findFirst { it.gameData.description },
            releaseDate = dataByBasicPriority.findFirst { it.gameData.releaseDate },

            criticScore = dataByScorePriority.findFirst { it.gameData.criticScore },
            userScore = dataByScorePriority.findFirst { it.gameData.userScore },

            genres = dataByBasicPriority.flatMapTo(mutableSetOf<String>()) { it.gameData.genres }.toList()
        )
    }

    private fun RawGame.toImageUrls(): ImageUrls {
        val dataByImagePriority = this.providerDataBy { it.imagePriorty }

        val thumbnailUrl = dataByImagePriority.findFirst { it.imageUrls.thumbnailUrl }
        val posterUrl = dataByImagePriority.findFirst { it.imageUrls.posterUrl }
        val screenshotUrls = dataByImagePriority.asSequence().flatMap { it.imageUrls.screenshotUrls.asSequence() }.take(maxScreenshots).toList()
        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderData(): List<ProviderData> = this.providerData.map { it.providerData }

    private fun RawGame.providerDataBy(f: (DataProviderType) -> Int) = providerData.sortedBy { f(it.providerData.type) }

    private fun <T> List<ProviderFetchResult>.findFirst(extractor: (ProviderFetchResult) -> T?): T? =
        this.asSequence().map(extractor).firstNotNull()
}