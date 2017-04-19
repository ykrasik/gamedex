package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.common.util.listFromJson
import com.gitlab.ykrasik.gamedex.datamodel.GamePlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:13
 */
@Singleton
open class IgdbClient @Inject constructor(private val config: IgdbConfig) {
    open fun search(name: String, platform: GamePlatform): List<Igdb.SearchResult> {
        val response = getRequest(config.endpoint,
            "search" to name,
            "filter[release_dates.platform][eq]" to platform.id.toString(),
            "limit" to config.maxSearchResults.toString(),
            "fields" to searchFieldsStr
        )
        return response.listFromJson()
    }

    open fun fetch(url: String): Igdb.DetailsResult {
        val response = getRequest(url,
            "fields" to fetchDetailsFieldsStr
        )

        // IGDB returns a list, even though we're fetching by id :/
        return response.listFromJson<Igdb.DetailsResult> { parseError(it) }.first()
    }

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = parameters.toMap(),
        headers = mapOf(
            "Accept" to "application/json",
            "X-Mashape-Key" to config.apiKey
        )
    )

    private fun parseError(raw: String): String {
        val errors: List<Igdb.Error> = raw.listFromJson()
        return errors.first().error.first()
    }

    private val GamePlatform.id: Int get() = config.getPlatformId(this)

    companion object {
        val searchFields = listOf(
            "name",
            "aggregated_rating",
            "release_dates.category",
            "release_dates.human",
            "release_dates.platform",
            "cover.cloudinary_id"
        )
        private val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = searchFields + listOf(
            "url",
            "summary",
            "rating",
            "screenshots.cloudinary_id",
            "genres"
        )
        private val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }
}