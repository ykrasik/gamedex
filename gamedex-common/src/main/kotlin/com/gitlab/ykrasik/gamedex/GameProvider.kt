package com.gitlab.ykrasik.gamedex

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface GameProvider {
    val type: GameProviderType
    val logo: ByteArray
    val supportedPlatforms: List<Platform>

    fun search(name: String, platform: Platform): List<ProviderSearchResult>
    fun download(apiUrl: String, platform: Platform): ProviderData
}

// TODO: I think it's possible to live without this enum.
enum class GameProviderType {
    Igdb,
    GiantBomb
}

data class ProviderSearchResult(
    val name: String,
    val releaseDate: String?,
    val score: Score?,
    val thumbnailUrl: String?,
    val apiUrl: String
)