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
        // TODO: Consider limiting the max amount of genres to 5.
        val genres = rawGameData.flatMapTo(mutableSetOf<String>()) {
            it.gameData.genres.flatMap { processGenre(it) }
        }.toList()
        return GameData(
            name = firstBy(preferences.nameOrder, priorityOverride?.name) { it.gameData.name } ?: metaData.path.name,
            description = firstBy(preferences.descriptionOrder, priorityOverride?.description) { it.gameData.description },
            releaseDate = firstBy(preferences.releaseDateOrder, priorityOverride?.releaseDate) { it.gameData.releaseDate },
            criticScore = firstBy(preferences.criticScoreOrder, priorityOverride?.criticScore) { it.gameData.criticScore },
            userScore = firstBy(preferences.userScoreOrder, priorityOverride?.userScore) { it.gameData.userScore },
            genres = genres
        )
    }

    private fun RawGame.toImageUrls(): ImageUrls {
        val thumbnailUrl = firstBy(preferences.thumbnailOrder, priorityOverride?.thumbnail) { it.imageUrls.thumbnailUrl }
        val posterUrl = firstBy(preferences.posterOrder, priorityOverride?.poster) { it.imageUrls.posterUrl }
        val screenshotUrls = sortDataBy(preferences.screenshotOrder, priorityOverride?.screenshots)
            .asSequence().flatMap { it.imageUrls.screenshotUrls.asSequence() }.take(maxScreenshots).toList()

        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderData(): List<ProviderData> = this.rawGameData.map { it.providerData }

    private fun <T> RawGame.firstBy(defaultOrder: DefaultProviderOrder, override: GameProviderType?, extractor: (RawGameData) -> T?): T? =
        sortDataBy(defaultOrder, override).findFirst(extractor)

    private fun RawGame.sortDataBy(order: DefaultProviderOrder, override: GameProviderType?) = rawGameData.sortedByDescending {
        val type = it.providerData.type
        if (type == override) {
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