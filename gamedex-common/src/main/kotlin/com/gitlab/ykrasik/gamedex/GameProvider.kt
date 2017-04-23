package com.gitlab.ykrasik.gamedex

import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface GameProvider {
    val info: GameProviderInfo

    fun search(name: String, platform: GamePlatform): List<ProviderSearchResult>

    fun fetch(apiUrl: String, platform: GamePlatform): RawGameData
}

enum class GameProviderType {
    Igdb,
    GiantBomb
}

class GameProviderInfo(
    val name: String,
    val type: GameProviderType,
    val logo: ByteArray
)

data class ProviderSearchResult(
    val name: String,
    val releaseDate: LocalDate?,
    val score: Double?,
    val thumbnailUrl: String?,
    val apiUrl: String
)