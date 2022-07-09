/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.provider.opencritic

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.util.httpClient
import io.ktor.client.request.*
import org.joda.time.DateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 28/09/2019
 * Time: 22:13
 */
@Singleton
open class OpenCriticClient @Inject constructor(private val config: OpenCriticConfig) {
    open suspend fun search(query: String): List<SearchResult> = get<List<SearchResult>>(
        endpoint = "${config.baseUrl}/api/meta/search",
        params = mapOf("criteria" to query)
    ).filter { it.relation == "game" }

    open suspend fun fetch(providerGameId: String): FetchResult = get(
        endpoint = "${config.baseUrl}/api/game/${providerGameId}",
        params = emptyMap()
    )

    private suspend inline fun <reified T : Any> get(endpoint: String, params: Map<String, String>): T =
        httpClient.get(endpoint) {
            params.forEach { parameter(it.key, it.value) }
            header("origin", "https://opencritic.com")
            header("referer", "https://opencritic.com/")
            header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.5060.114 Safari/537.36"
            )
            header("authority", "api.opencritic.com")
            header("dnt", 1)
            header("pragma", "no-cache")
            header("sec-fetch-dest", "empty")
            header("sec-fetch-mode", "cors")
            header("sec-fetch-site", "same-site")
            header("sec-gpc", 1)
        }

    data class SearchResult(
        val id: Int,
        val name: String,
        val dist: Double,
        val relation: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class FetchResult(
        val id: Int,
        val name: String,
        val description: String?,
        val medianScore: Double,
        val numReviews: Int,
        val Genres: List<Genre>,
        val Platforms: List<Platform>,
        val firstReleaseDate: DateTime?,
        val verticalLogoScreenshot: Image?,
        val bannerScreenshot: Image?,
        val logoScreenshot: Image?,
        val mastheadScreenshot: Image?,
        val screenshots: List<Image>,
    )

    data class Image(
        val fullRes: String,
        val thumbnail: String?,
    )

    data class Genre(
        val id: Int,
        val name: String,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Platform(
        val id: Int,
        val shortName: String,
        val releaseDate: DateTime?,
    )
}
