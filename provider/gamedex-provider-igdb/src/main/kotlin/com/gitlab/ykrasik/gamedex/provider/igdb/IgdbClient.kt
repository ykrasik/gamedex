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

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.httpClient
import com.gitlab.ykrasik.gamedex.util.listFromJson
import io.ktor.client.request.header
import io.ktor.client.request.post
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:13
 */
@Singleton
open class IgdbClient @Inject constructor(private val config: IgdbConfig) {
    open suspend fun search(query: String, platform: Platform, account: IgdbUserAccount): List<SearchResult> =
        post(account) {
            // Split by non-word characters
            val queryWords = query.split("[\\W]".toRegex()).asSequence()
                .filter { it.isNotBlank() }
                .joinToString(" & ") { """name ~ *"$it"*""" }
            """
                search "$query";
                fields $searchFieldsStr;
                where $queryWords & release_dates.platform = ${platform.id};
                limit ${config.maxSearchResults};
            """.trimIndent()
        }

    open suspend fun fetch(providerGameId: String, account: IgdbUserAccount): DetailsResult? =
        post<DetailsResult>(account) {
            """
                fields $fetchDetailsFieldsStr;
                where id = $providerGameId;
            """.trimIndent()
        }.firstOrNull()

    private suspend inline fun <reified T : Any> post(account: IgdbUserAccount, body: () -> String): List<T> {
        // Ktor fails to correctly parse a list of data classes, we have to do it manually.
        val response = httpClient.post<ByteArray>(config.baseUrl) {
            header("user-key", account.apiKey)
            this.body = body()
        }
        return response.listFromJson()
    }

    private val Platform.id: Int get() = config.getPlatformId(this)

    private companion object {
        val searchFields = listOf(
            "name",
            "summary",
            "aggregated_rating",
            "aggregated_rating_count",
            "rating",
            "rating_count",
            "release_dates.category",
            "release_dates.human",
            "release_dates.platform",
            "cover.image_id"
        )
        val searchFieldsStr = searchFields.joinToString(",")

        val fetchDetailsFields = searchFields + listOf(
            "url",
            "screenshots.image_id",
            "genres"
        )
        val fetchDetailsFieldsStr = fetchDetailsFields.joinToString(",")
    }

    @JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy::class)
    data class SearchResult(
        val id: Int,
        val name: String,
        val summary: String?,
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
        val imageId: String?
    )
}