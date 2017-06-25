package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
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
    private val settings: ProviderSettings
) {
    private val maxScreenshots = 10
    private val maxGenres = 7

    fun create(rawGame: RawGame): Game {
        val library = libraryRepository[rawGame.metaData.libraryId]
        val gameData = rawGame.toGameData()
        val imageUrls = rawGame.toImageUrls()
        val providerHeaders = rawGame.toProviderHeaders()
        val folderMetaData = NameHandler.analyze(rawGame.metaData.path.name)

        return Game(
            library = library,
            rawGame = rawGame,
            gameData = gameData,
            providerHeaders = providerHeaders,
            imageUrls = imageUrls,
            folderMetaData = folderMetaData
        )
    }

    private fun RawGame.toGameData(): GameData = GameData(
        siteUrl = "", // Not used.
        name = firstBy(settings.nameOrder, userData?.nameOverride()) { it.gameData.name } ?: metaData.path.name,
        description = firstBy(settings.descriptionOrder, userData?.descriptionOverride()) { it.gameData.description },
        releaseDate = firstBy(settings.releaseDateOrder, userData?.releaseDateOverride()) { it.gameData.releaseDate },
        // TODO: Choose score with most votes.
        criticScore = firstBy(settings.criticScoreOrder, userData?.criticScoreOverride(), { Score(it as Double, 1) }) {
            it.gameData.criticScore.minOrNull()
        },
        userScore = firstBy(settings.userScoreOrder, userData?.userScoreOverride(), { Score(it as Double, 1) }) {
            it.gameData.userScore.minOrNull()
        },
        genres = unsortedListBy(userData?.genresOverride()) { it.gameData.genres }.flatMap { processGenre(it) }.distinct().take(maxGenres)
    )

    private fun Score?.minOrNull() = this?.let { if (it.numReviews >= 4) it else null }

    private fun RawGame.toImageUrls(): ImageUrls {
        val thumbnailUrl = firstBy(settings.thumbnailOrder, userData?.thumbnailOverride()) { it.imageUrls.thumbnailUrl }
        val posterUrl = firstBy(settings.posterOrder, userData?.posterOverride()) { it.imageUrls.posterUrl }
        val screenshotUrls = listBy(settings.screenshotOrder, userData?.screenshotsOverride()) { it.imageUrls.screenshotUrls }.take(maxScreenshots)

        return ImageUrls(
            thumbnailUrl = thumbnailUrl ?: posterUrl,
            posterUrl = posterUrl ?: thumbnailUrl,
            screenshotUrls = screenshotUrls
        )
    }

    private fun RawGame.toProviderHeaders(): List<ProviderHeader> = this.providerData.map { it.header }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.firstBy(defaultOrder: ProviderSettings.Order,
                                    override: GameDataOverride?,
                                    converter: (Any) -> T = { it as T },
                                    extractor: (ProviderData) -> T?): T? =
        when (override) {
            is GameDataOverride.Custom -> converter(override.data)
            else -> {
                val sorted = sortDataBy(defaultOrder, override as? GameDataOverride.Provider)
                sorted.findFirst(extractor)
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T> RawGame.listBy(defaultOrder: ProviderSettings.Order, override: GameDataOverride?, extractor: (ProviderData) -> List<T>): List<T> =
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

    private fun RawGame.sortDataBy(order: ProviderSettings.Order, override: GameDataOverride.Provider?): List<ProviderData> =
        providerData.sortedBy {
            val type = it.header.id
            if (type == override?.provider) {
                ProviderSettings.Order.minOrder
            } else {
                order[type]
            }
        }

    private fun <T> List<ProviderData>.findFirst(extractor: (ProviderData) -> T?): T? =
        this.asSequence().map(extractor).firstNotNull()

    // TODO: This looks like something that can sit as configuration, maybe configurable through a UI.
    private fun processGenre(genre: String): List<String> = when (genre) {
        "Action-Adventure", "Action Adventure" ->
            listOf("Action", "Adventure")

        "Action RPG" ->
            listOf("Action", "Role-Playing Game (RPG)")

        "Flight Simulator", "Small Spaceship", "Large Spaceship", "Virtual Life" ->
            listOf("Simulation")

        "Real-Time Strategy" ->
            listOf("Real Time Strategy (RTS)")

        "Turn-Based" ->
            listOf("Turn-Based Strategy (TBS)")

        "First-Person" ->
            listOf("First-Person Shooter")

        "Shoot 'Em Up", "Shoot-'Em-Up", "Artillery" ->
            listOf("Shooter")

        "Music/Rhythm", "Music", "Rhythm" ->
            listOf("Music / Rhythm")

        "Minigame Collection" ->
            listOf("Compilation")

        "Defense", "Military", "Command", "Tactical", "Tactics", "Wargame" ->
            listOf("Strategy")

        "Logic", "Matching" ->
            listOf("Puzzle")

        "Text Adventure" ->
            listOf("Adventure")

        "City Building", "Business / Tycoon", "Tycoon", "Breeding/Constructing", "Career", "Government" ->
            listOf("Management")

        "Role-Playing", "Console-style RPG", "PC-style RPG", "Japanese-Style", "Western-Style" ->
            listOf("Role-Playing Game (RPG)")

        "Hack & Slash/Beat 'Em Up", "Beat-'Em-Up", "Brawler", "Fighting" ->
            listOf("Hack & Slash / Beat 'Em Up")

        "Sports", "Football", "Boxing", "Bowling", "Golf" ->
            listOf("Sport")

        "Board Games", "Card Battle", "Card Game", "Gambling", "Quiz/Trivia", "Parlor", "Trivia/Board Game" ->
            listOf("Board / Card Game")

        "Driving/Racing", "Automobile", "Car Combat", "GT / Street", "Motocross", "Motorcycle", "Racing", "Vehicle",
        "Vehicular Combat", "Driving" ->
            listOf("Driving / Racing")

        "General", "Miscellaneous", "Modern", "Traditional", "Horizontal", "Vertical", "Virtual", "Linear", "Other",
        "Scrolling", "Static", "Civilian", "Individual", "Team", "3D", "Combat", "Dual-Joystick Shooter", "Educational",
        "Real-Time", "Fishing", "Train", "Rail", "Light Gun", "Block-Breaking",
        "Historic", "Futuristic", "Fantasy", "Sci-Fi", "Space" ->
            emptyList()

        else ->
            listOf(genre)
    }
}