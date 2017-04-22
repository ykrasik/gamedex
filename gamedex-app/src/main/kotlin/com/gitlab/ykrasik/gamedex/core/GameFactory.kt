package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.ProviderPriority
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import com.gitlab.ykrasik.gamedex.util.firstNotNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 20/04/2017
 * Time: 20:35
 */
@Singleton
class GameFactory @Inject constructor(private val userPreferences: UserPreferences) {
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

    private fun RawGame.toGameData(): GameData = GameData(
        name = sortDataBy(userPreferences.providerNamePriority).findFirst { it.gameData.name } ?: metaData.path.name,
        description = sortDataBy(userPreferences.providerDescriptionPriority).findFirst { it.gameData.description },
        releaseDate = sortDataBy(userPreferences.providerReleaseDatePriority).findFirst { it.gameData.releaseDate },

        criticScore = sortDataBy(userPreferences.providerCriticScorePriority).findFirst { it.gameData.criticScore },
        userScore = sortDataBy(userPreferences.providerUserScorePriority).findFirst { it.gameData.userScore },

        genres = providerData.flatMapTo(mutableSetOf<String>()) { it.gameData.genres }.toList()
    )

    private fun RawGame.toImageUrls(): ImageUrls {
        val thumbnailUrl = sortDataBy(userPreferences.providerThumbnailPriority).findFirst { it.imageUrls.thumbnailUrl }
        val posterUrl = sortDataBy(userPreferences.providerPosterPriority).findFirst { it.imageUrls.posterUrl }
        val screenshotUrls = sortDataBy(userPreferences.providerScreenshotPriority)
            .asSequence().flatMap { it.imageUrls.screenshotUrls.asSequence() }.take(maxScreenshots).toList()

        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderData(): List<ProviderData> = this.providerData.map { it.providerData }

    private fun RawGame.sortDataBy(preference: ProviderPriority) = providerData.sortedByDescending {
        preference[it.providerData.type]
    }

    private fun <T> List<ProviderFetchResult>.findFirst(extractor: (ProviderFetchResult) -> T?): T? =
        this.asSequence().map(extractor).firstNotNull()
}