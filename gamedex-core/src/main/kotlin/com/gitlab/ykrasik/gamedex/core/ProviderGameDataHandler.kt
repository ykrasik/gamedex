package com.gitlab.ykrasik.gamedex.core

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.github.ykrasik.gamedex.datamodel.ProviderGameData
import com.gitlab.ykrasik.gamedex.persistence.RawGame
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 22/01/2017
 * Time: 19:41
 */
@Singleton
class ProviderGameDataHandler @Inject constructor() {
    private val log by logger()

    fun createGame(rawGame: RawGame): Game = with(rawGame) {
        check(providerData.isNotEmpty()) { "Provider data is empty: Can't construct a game without provider data!" }

        val basicData = providerData.sortedBy { it.type.basicDataPriority }
        val scoreData = providerData.sortedBy { it.type.scorePriority }

        val gameName = basicData.first().data.name
        return Game(
            id = id,
            metaData = metaData,
            data = GameData(
                name = gameName,
                description = basicData.findFirst(gameName, "description") { it.data.description },
                releaseDate = basicData.findFirst(gameName, "releaseDate") { it.data.releaseDate },

                criticScore = scoreData.findFirst(gameName, "criticScore") { it.data.criticScore },
                userScore = scoreData.findFirst(gameName, "userScore") { it.data.userScore },

                genres = providerData.flatMapTo(mutableSetOf<String>()) { it.data.genres }.toList()
            )
        )
    }

    fun chooseGameImageData(gameName: String, providerData: List<ProviderGameData>): GameImageData {
        val imageData = providerData.sortedBy { it.type.imagePriorty }
        return GameImageData(
            thumbnailUrl = imageData.findFirst(gameName, "thumbnail") { it.imageData.thumbnailUrl },
            posterUrl = imageData.findFirst(gameName, "poster") { it.imageData.posterUrl },
            screenshot1Url = imageData.findFirst(gameName, "screenshot1Url") { it.imageData.screenshot1Url },
            screenshot2Url = imageData.findFirst(gameName, "screenshot2Url") { it.imageData.screenshot2Url },
            screenshot3Url = imageData.findFirst(gameName, "screenshot3Url") { it.imageData.screenshot3Url },
            screenshot4Url = imageData.findFirst(gameName, "screenshot4Url") { it.imageData.screenshot4Url },
            screenshot5Url = imageData.findFirst(gameName, "screenshot5Url") { it.imageData.screenshot5Url },
            screenshot6Url = imageData.findFirst(gameName, "screenshot6Url") { it.imageData.screenshot6Url },
            screenshot7Url = imageData.findFirst(gameName, "screenshot7Url") { it.imageData.screenshot7Url },
            screenshot8Url = imageData.findFirst(gameName, "screenshot8Url") { it.imageData.screenshot8Url },
            screenshot9Url = imageData.findFirst(gameName, "screenshot9Url") { it.imageData.screenshot9Url },
            screenshot10Url = imageData.findFirst(gameName, "screenshot10Url") { it.imageData.screenshot10Url }
        )
    }

    private fun <T> List<ProviderGameData>.findFirst(gameName: String, field: String, extractor: (ProviderGameData) -> T?): T? {
        val providerData = this.firstOrNull { extractor(it) != null }
        return if (providerData != null) {
            val value = extractor(providerData)
            log.debug { "[$gameName][$field][${providerData.type}]: $value" }
            value
        } else {
            log.debug { "[$gameName][$field]: Empty." }
            null
        }
    }
}