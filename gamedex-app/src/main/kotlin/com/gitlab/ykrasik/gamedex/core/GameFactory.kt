package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.preferences.DefaultProviderOrder
import com.gitlab.ykrasik.gamedex.preferences.ProviderPreferences
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
    private val preferences: ProviderPreferences
) {
    private val maxScreenshots = 10
    private val maxGenres = 7

    fun create(rawGame: RawGame): Game {
        val library = libraryRepository[rawGame.metaData.libraryId]
        val gameData = rawGame.toGameData()
        val imageUrls = rawGame.toImageUrls()
        val providerHeaders = rawGame.toProviderHeaders()

        return Game(
            library = library,
            rawGame = rawGame,
            gameData = gameData,
            providerHeaders = providerHeaders,
            imageUrls = imageUrls
        )
    }

    private fun RawGame.toGameData(): GameData = GameData(
        name = firstBy(preferences.nameOrder, userData?.nameOverride()) { it.gameData.name } ?: metaData.path.name,
        description = firstBy(preferences.descriptionOrder, userData?.descriptionOverride()) { it.gameData.description },
        releaseDate = firstBy(preferences.releaseDateOrder, userData?.releaseDateOverride()) { it.gameData.releaseDate },
        criticScore = firstBy(preferences.criticScoreOrder, userData?.criticScoreOverride()) { it.gameData.criticScore },
        userScore = firstBy(preferences.userScoreOrder, userData?.userScoreOverride()) { it.gameData.userScore },
        genres = unsortedListBy(userData?.genresOverride()) { it.gameData.genres }.flatMap { processGenre(it) }.distinct().take(maxGenres)
    )

    private fun RawGame.toImageUrls(): ImageUrls {
        val thumbnailUrl = firstBy(preferences.thumbnailOrder, userData?.thumbnailOverride()) { it.imageUrls.thumbnailUrl }
        val posterUrl = firstBy(preferences.posterOrder, userData?.posterOverride()) { it.imageUrls.posterUrl }
        val screenshotUrls = listBy(preferences.screenshotOrder, userData?.screenshotsOverride()) { it.imageUrls.screenshotUrls }.take(maxScreenshots)

        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderHeaders(): List<ProviderHeader> = this.providerData.map { it.header }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.firstBy(defaultOrder: DefaultProviderOrder, override: GameDataOverride?, extractor: (ProviderData) -> T?): T? =
        when (override) {
            is GameDataOverride.Custom -> override.data as T
            else -> {
                val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
                sorted.findFirst(extractor)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.listBy(defaultOrder: DefaultProviderOrder, override: GameDataOverride?, extractor: (ProviderData) -> List<T>): List<T> =
        when (override) {
            is GameDataOverride.Custom -> override.data as List<T>
            else -> {
                val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
                sorted.flatMap(extractor)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.unsortedListBy(override: GameDataOverride?, extractor: (ProviderData) -> List<T>): List<T> =
        when (override) {
            is GameDataOverride.Custom -> override.data as List<T>
            else -> providerData.flatMap(extractor)
        }

    private fun RawGame.sortDataBy(order: DefaultProviderOrder, override: GameDataOverride.Provider?): List<ProviderData> =
        providerData.sortedByDescending {
            val type = it.header.type
            if (type == override?.provider) {
                DefaultProviderOrder.maxPriority + 1
            } else {
                order[type]
            }
        }

    private fun <T> List<ProviderData>.findFirst(extractor: (ProviderData) -> T?): T? =
        this.asSequence().map(extractor).firstNotNull()

    private fun processGenre(genre: String): List<String> = when (genre) {
        "Action-Adventure" -> listOf("Action", "Adventure")
        "Driving/Racing" -> listOf("Racing")
        "Dual-Joystick Shooter" -> emptyList()
        "Educational" -> emptyList()
        "Fishing" -> emptyList()
        "Flight Simulator" -> listOf("Simulation")
        "Football" -> listOf("Sport")
        "Music/Rhythm" -> listOf()
        "Real-Time Strategy" -> listOf("Real Time Strategy (RTS)")
        "Role-Playing" -> listOf("Role-Playing Game (RPG)")
        else -> listOf(genre)
    }
}