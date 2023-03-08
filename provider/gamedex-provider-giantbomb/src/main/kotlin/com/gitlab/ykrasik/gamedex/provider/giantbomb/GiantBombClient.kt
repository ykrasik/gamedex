/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.httpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:33
 */
@Singleton
open class GiantBombClient @Inject constructor(private val config: GiantBombConfig) {
    open suspend fun search(
        query: String,
        platform: Platform,
        account: GiantBombUserAccount,
        offset: Int,
        limit: Int,
    ): SearchResponse = get(
        endpoint = config.baseUrl,
        account = account,
        params = mapOf(
            "filter" to "name:$query,platforms:${platform.id}",
            "field_list" to searchFieldsStr,
            "offset" to "$offset",
            "limit" to "$limit"
        )
    )

    open suspend fun fetch(apiUrl: String, account: GiantBombUserAccount): FetchResponse = get(
        endpoint = apiUrl,
        account = account,
        params = mapOf("field_list" to fetchDetailsFieldsStr)
    )

    private val Platform.id: Int get() = config.getPlatformId(this)

    private suspend inline fun <reified T : Any> get(endpoint: String, account: GiantBombUserAccount, params: Map<String, String>): T =
        httpClient.get(endpoint) {
            parameter("api_key", account.apiKey)
            parameter("format", "json")
            params.forEach { parameter(it.key, it.value) }
        }.body()

    private companion object {
        val searchFields = listOf(
            "api_detail_url",
            "name",
            "deck",
            "original_release_date",
            "expected_release_year",
            "expected_release_quarter",
            "expected_release_month",
            "expected_release_day",
            "image"
        )
        val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = searchFields - "api_detail_url" + listOf(
            "site_detail_url",
            "genres",
            "images"
        )
        val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class SearchResponse(
        val statusCode: Status,
        val results: List<SearchResult>,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class SearchResult(
        val apiDetailUrl: String,
        val name: String,
        val deck: String?,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        override val originalReleaseDate: LocalDate?,
        override val expectedReleaseYear: Int?,
        override val expectedReleaseQuarter: Int?,
        override val expectedReleaseMonth: Int?,
        override val expectedReleaseDay: Int?,
        val image: Image?,
    ) : HasReleaseDate

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class FetchResponse(
        val statusCode: Status,

        // When result is found - GiantBomb returns a Json object. When result is not found, GiantBomb returns an empty Json array []. Annoying.
        @JsonFormat(with = [(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)])
        val results: List<FetchResult>,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class FetchResult(
        val siteDetailUrl: String,
        val name: String,
        val deck: String?,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        override val originalReleaseDate: LocalDate?,
        override val expectedReleaseYear: Int?,
        override val expectedReleaseQuarter: Int?,
        override val expectedReleaseMonth: Int?,
        override val expectedReleaseDay: Int?,
        val image: Image?,
        val images: List<Image>,
        val genres: List<Genre>?,
    ) : HasReleaseDate

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Genre(
        val name: String,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Image(
        val thumbUrl: String,
        val superUrl: String,
    )

    interface HasReleaseDate {
        val originalReleaseDate: LocalDate?
        val expectedReleaseYear: Int?
        val expectedReleaseQuarter: Int?
        val expectedReleaseMonth: Int?
        val expectedReleaseDay: Int?
    }

    enum class Status(val statusCode: Int) {
        OK(1),
        InvalidApiKey(100),
        NotFound(101),
        BadFormat(102),
        JsonPNoCallback(103),
        FilterError(104),
        VideoOnlyForSubscribers(105);

        override fun toString() = "$name($statusCode)"

        companion object {
            @JsonCreator
            @JvmStatic
            operator fun invoke(code: Int): Status = Status::class.java.enumConstants.find { it.statusCode == code }!!
        }
    }
}
