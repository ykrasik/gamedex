/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.isSuccess
import com.gitlab.ykrasik.gamedex.util.listFromJson
import com.gitlab.ykrasik.gamedex.util.logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:13
 */
@Singleton
open class IgdbClient @Inject constructor(private val config: IgdbConfig) {
    private val log = logger()

    open fun search(query: String, platform: Platform, account: IgdbUserAccount): List<SearchResult> = get(
        endpoint = "${config.baseUrl}/",
        account = account,
        initialMessage = "[$platform] Searching '$query'...",
        params = mapOf(
            "search" to query,
            "filter[release_dates.platform][eq]" to platform.id.toString(),
            "limit" to config.maxSearchResults.toString(),
            "fields" to searchFieldsStr
        )
    ) { it.listFromJson() }

    open fun fetch(apiUrl: String, account: IgdbUserAccount): DetailsResult = get<DetailsResult>(
        endpoint = apiUrl,
        account = account,
        initialMessage = "Fetching '$apiUrl'...",
        params = mapOf("fields" to fetchDetailsFieldsStr)
    ) {
        // IGDB returns a list, even though we're fetching by id :/
        val result = it.listFromJson<DetailsResult>()
        return result.firstOrNull() ?: throw IllegalStateException("Fetch '$apiUrl': Not Found!")
    }

    private inline fun <reified T : Any> get(endpoint: String, account: IgdbUserAccount, initialMessage: String, params: Map<String, String>, transform: (String) -> T): T {
        log.trace(initialMessage)
        val response = khttp.get(endpoint, params = params, headers = mapOf("Accept" to "application/json", "user-key" to account.apiKey))
        val text = response.text
        val message = "$initialMessage Done: [${response.statusCode}] $text"
        log.trace(message)
        return if (response.isSuccess) {
            transform(text)
        } else {
            throw IllegalStateException(message)
        }
    }

//    private fun parseError(raw: String): String {
//        return if (raw.startsWith("{")) {
//            val errors: List<Error> = raw.listFromJson()
//            return errors.first().error.first()
//        } else {
//            raw
//        }
//    }

    private val Platform.id: Int get() = config.getPlatformId(this)

    private companion object {
        val searchFields = listOf(
            "name",
            "aggregated_rating",
            "aggregated_rating_count",
            "rating",
            "rating_count",
            "release_dates.category",
            "release_dates.human",
            "release_dates.platform",
            "cover.cloudinary_id"
        )
        val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = searchFields + listOf(
            "url",
            "summary",
            "screenshots.cloudinary_id",
            "genres"
        )
        val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    data class SearchResult(
        val id: Int,
        val name: String,
        val aggregatedRating: Double?,
        val aggregatedRatingCount: Int?,
        val rating: Double?,
        val ratingCount: Int?,
        val releaseDates: List<ReleaseDate>?,
        val cover: Image?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ReleaseDate(
        val platform: Int,
        val category: Int,
        val human: String
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailsResult(
        val url: String,
        val name: String,
        val summary: String?,
        val releaseDates: List<ReleaseDate>?,
        val aggregatedRating: Double?,
        val aggregatedRatingCount: Int?,
        val rating: Double?,
        val ratingCount: Int?,
        val cover: Image?,
        val screenshots: List<Image>?,
        val genres: List<Int>?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Image(
        val cloudinaryId: String?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Error(
        val error: List<String>
    )
}