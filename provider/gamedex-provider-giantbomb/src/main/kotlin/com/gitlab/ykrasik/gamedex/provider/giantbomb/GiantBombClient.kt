/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.fromJson
import com.gitlab.ykrasik.gamedex.util.logger
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.parameter
import io.ktor.client.response.readText
import org.joda.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 19/04/2017
 * Time: 09:33
 */
// TODO: GiantBomb can return a number of user reviews
@Singleton
open class GiantBombClient @Inject constructor(private val config: GiantBombConfig, private val client: HttpClient) {
    private val log = logger()

    open suspend fun search(query: String, platform: Platform, account: GiantBombUserAccount): SearchResponse = get(
        endpoint = config.baseUrl,
        account = account,
        initialMessage = "Searching [$platform] '$query'...",
        params = mapOf(
            "filter" to "name:$query,platforms:${platform.id}",
            "field_list" to searchFieldsStr
        )
    )

    open suspend fun fetch(apiUrl: String, account: GiantBombUserAccount): DetailsResponse = get(
        endpoint = apiUrl,
        account = account,
        initialMessage = "Fetching '$apiUrl'...",
        params = mapOf("field_list" to fetchDetailsFieldsStr)
    )

    private val Platform.id: Int get() = config.getPlatformId(this)

    private suspend inline fun <reified T : Any> get(endpoint: String, account: GiantBombUserAccount, initialMessage: String, params: Map<String, String>): T {
        log.trace(initialMessage)
        val response = client.call(endpoint) {
            parameter("api_key", account.apiKey)
            parameter("format", "json")
            params.forEach { parameter(it.key, it.value) }
        }.response
        val text = response.readText()
        val message = "$initialMessage Done: [${response.status}] $text"
        log.trace(message)
        return text.fromJson()
    }

    private companion object {
        val searchFields = listOf(
            "api_detail_url",
            "name",
            "deck",
            "original_release_date",
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

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SearchResponse(
        val statusCode: Status,
        val results: List<SearchResult>
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    data class SearchResult(
        val apiDetailUrl: String,
        val name: String,
        val deck: String?,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")   // TODO: Can I just use a string and split by space?
        val originalReleaseDate: LocalDate?,
        val image: Image?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DetailsResponse(
        val statusCode: Status,

        // When result is found - GiantBomb returns a Json object. When result is not found, GiantBomb returns an empty Json array []. Annoying.
        @JsonFormat(with = [(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)])
        val results: List<DetailsResult>
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    data class DetailsResult(
        val siteDetailUrl: String,
        val name: String,
        val deck: String?,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        val originalReleaseDate: LocalDate?,
        val image: Image?,
        val images: List<Image>,
        val genres: List<Genre>?
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Genre(
        val name: String
    )

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Image(
        val thumbUrl: String,
        val superUrl: String
    )

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