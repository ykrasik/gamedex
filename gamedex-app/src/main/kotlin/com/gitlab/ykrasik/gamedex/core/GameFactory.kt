package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.preferences.DefaultProviderPriority
import com.gitlab.ykrasik.gamedex.preferences.UserPreferences
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
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
    private val libraryRepository: LibraryRepository,
    private val userPreferences: UserPreferences
) {
    private val maxScreenshots = 10

    fun create(rawGame: RawGame): Game {
        val library = libraryRepository[rawGame.metaData.libraryId]
        val gameData = rawGame.toGameData()
        val imageUrls = rawGame.toImageUrls()
        val providerData = rawGame.toProviderData()

        return Game(
            library = library,
            rawGame = rawGame,
            gameData = gameData,
            providerData = providerData,
            imageUrls = imageUrls
        )
    }

    private fun RawGame.toGameData(): GameData = GameData(
        name = topPriority(userPreferences.providerNamePriority, priorityOverride?.name) { it.gameData.name } ?: metaData.path.name,
        description = topPriority(userPreferences.providerDescriptionPriority, priorityOverride?.description) { it.gameData.description },
        releaseDate = topPriority(userPreferences.providerReleaseDatePriority, priorityOverride?.releaseDate) { it.gameData.releaseDate },
        criticScore = topPriority(userPreferences.providerCriticScorePriority, priorityOverride?.criticScore) { it.gameData.criticScore },
        userScore = topPriority(userPreferences.providerUserScorePriority, priorityOverride?.userScore) { it.gameData.userScore },
        genres = rawGameData.flatMapTo(mutableSetOf<String>()) { it.gameData.genres }.toList()  // TODO: Consider limiting the max amount of genres to 5.
    )

    private fun RawGame.toImageUrls(): ImageUrls {
        val thumbnailUrl = topPriority(userPreferences.providerThumbnailPriority, priorityOverride?.thumbnail) { it.imageUrls.thumbnailUrl }
        val posterUrl = topPriority(userPreferences.providerPosterPriority, priorityOverride?.poster) { it.imageUrls.posterUrl }
        val screenshotUrls = sortDataBy(userPreferences.providerScreenshotPriority, priorityOverride?.screenshots)
            .asSequence().flatMap { it.imageUrls.screenshotUrls.asSequence() }.take(maxScreenshots).toList()

        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderData(): List<ProviderData> = this.rawGameData.map { it.providerData }

    private fun <T> RawGame.topPriority(defaultPriority: DefaultProviderPriority, override: GameProviderType?, extractor: (RawGameData) -> T?): T? =
        sortDataBy(defaultPriority, override).findFirst(extractor)

    private fun RawGame.sortDataBy(priority: DefaultProviderPriority, override: GameProviderType?) = rawGameData.sortedByDescending {
        val type = it.providerData.type
        if (type == override) {
            DefaultProviderPriority.maxPriority + 1
        } else {
            priority[type]
        }
    }

    private fun <T> List<RawGameData>.findFirst(extractor: (RawGameData) -> T?): T? =
        this.asSequence().map(extractor).firstNotNull()
}