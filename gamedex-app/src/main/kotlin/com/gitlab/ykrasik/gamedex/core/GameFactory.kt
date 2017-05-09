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
        val providerData = rawGame.toProviderData()

        return Game(
            library = library,
            rawGame = rawGame,
            gameData = gameData,
            providerData = providerData,
            imageUrls = imageUrls
        )
    }

    private fun RawGame.toGameData(): GameData {
        val overrides = userData?.overrides

        return GameData(
            name = firstBy(preferences.nameOrder, overrides?.name) { it.gameData.name } ?: metaData.path.name,
            description = firstBy(preferences.descriptionOrder, overrides?.description) { it.gameData.description },
            releaseDate = firstBy(preferences.releaseDateOrder, overrides?.releaseDate) { it.gameData.releaseDate },
            criticScore = firstBy(preferences.criticScoreOrder, overrides?.criticScore) { it.gameData.criticScore },
            userScore = firstBy(preferences.userScoreOrder, overrides?.userScore) { it.gameData.userScore },
            genres = unsortedListBy(overrides?.genres) { it.gameData.genres }.flatMap { processGenre(it) }.distinct().take(maxGenres)
        )
    }

    private fun RawGame.toImageUrls(): ImageUrls {
        val overrides = userData?.overrides

        val thumbnailUrl = firstBy(preferences.thumbnailOrder, overrides?.thumbnail) { it.imageUrls.thumbnailUrl }
        val posterUrl = firstBy(preferences.posterOrder, overrides?.poster) { it.imageUrls.posterUrl }
        val screenshotUrls = listBy(preferences.screenshotOrder, overrides?.screenshots) { it.imageUrls.screenshotUrls }.take(maxScreenshots)

        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderData(): List<ProviderData> = this.rawGameData.map { it.providerData }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.firstBy(defaultOrder: DefaultProviderOrder, override: GameDataOverride?, extractor: (RawGameData) -> T?): T? =
        when (override) {
            is GameDataOverride.Custom -> override.data as T
            else -> {
                val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
                sorted.findFirst(extractor)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.listBy(defaultOrder: DefaultProviderOrder, override: GameDataOverride?, extractor: (RawGameData) -> List<T>): List<T> =
        when (override) {
            is GameDataOverride.Custom -> override.data as List<T>
            else -> {
                val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
                sorted.flatMap(extractor)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.unsortedListBy(override: GameDataOverride?, extractor: (RawGameData) -> List<T>): List<T> =
        when (override) {
            is GameDataOverride.Custom -> override.data as List<T>
            else -> rawGameData.flatMap(extractor)
        }

    private fun RawGame.sortDataBy(order: DefaultProviderOrder, override: GameDataOverride.Provider?): List<RawGameData> =
        rawGameData.sortedByDescending {
            val type = it.providerData.type
            if (type == override?.provider) {
                DefaultProviderOrder.maxPriority + 1
            } else {
                order[type]
            }
        }

    private fun <T> List<RawGameData>.findFirst(extractor: (RawGameData) -> T?): T? =
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