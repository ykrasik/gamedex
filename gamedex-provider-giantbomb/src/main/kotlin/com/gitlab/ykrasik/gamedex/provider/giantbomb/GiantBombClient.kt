package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.gitlab.ykrasik.gamedex.common.util.fromJson
import com.gitlab.ykrasik.gamedex.datamodel.GamePlatform
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:33
 */
@Singleton
open class GiantBombClient @Inject constructor(private val config: GiantBombConfig) {
    open fun search(name: String, platform: GamePlatform): GiantBomb.SearchResponse {
        val response = getRequest(config.endpoint,
            "filter" to "name:$name,platforms:${platform.id}",
            "field_list" to searchFieldsStr
        )
        return response.fromJson()
    }

    open fun fetch(apiUrl: String): GiantBomb.DetailsResponse {
        val response = getRequest(apiUrl, "field_list" to fetchDetailsFieldsStr)
        return response.fromJson()
    }

    private val GamePlatform.id: Int get() = config.getPlatformId(this)

    private fun getRequest(path: String, vararg parameters: Pair<String, String>) = khttp.get(path,
        params = mapOf("api_key" to config.apiKey, "format" to "json", *parameters)
    )

    companion object {
        val searchFields = listOf(
            "api_detail_url",
            "name",
            "original_release_date",
            "image"
        )
        private val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = listOf(
            "site_detail_url",
            "description",
            "genres"
        )
        private val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }
}