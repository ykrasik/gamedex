package com.gitlab.ykrasik.gamedex

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:42
 */
interface GameProvider {
    val id: ProviderId
    val logo: ByteArray
    val supportedPlatforms: List<Platform>
    val defaultOrder: ProviderOrderPriorities

    val accountFeature: ProviderUserAccountFeature?

    fun search(name: String, platform: Platform, account: ProviderUserAccount?): List<ProviderSearchResult>
    fun download(apiUrl: String, platform: Platform, account: ProviderUserAccount?): ProviderData

    fun supports(platform: Platform) = supportedPlatforms.contains(platform)
}

typealias ProviderId = String

interface ProviderUserAccountFeature {
    val accountUrl: String
    val fields: List<String>
    fun createAccount(fields: Map<String, String>): ProviderUserAccount
}

interface ProviderUserAccount

// Higher number means lower priority
data class ProviderOrderPriorities(
    val search: Int,
    val name: Int,
    val description: Int,
    val releaseDate: Int,
    val criticScore: Int,
    val userScore: Int,
    val thumbnail: Int,
    val poster: Int,
    val screenshot: Int
) {
    companion object {
        val default = ProviderOrderPriorities(
            search = 9999,
            name = 9999,
            description = 9999,
            releaseDate = 9999,
            criticScore = 9999,
            userScore = 9999,
            thumbnail = 9999,
            poster = 9999,
            screenshot = 9999
        )
    }
}

data class ProviderSearchResult(
    val name: String,
    val releaseDate: String?,
    val criticScore: Score?,
    val userScore: Score?,
    val thumbnailUrl: String?,
    val apiUrl: String
)