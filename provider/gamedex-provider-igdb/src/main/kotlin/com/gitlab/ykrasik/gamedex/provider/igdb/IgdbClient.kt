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

package com.gitlab.ykrasik.gamedex.provider.igdb

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.util.SingleValueStorage
import com.gitlab.ykrasik.gamedex.util.httpClient
import com.gitlab.ykrasik.gamedex.util.listFromJson
import com.gitlab.ykrasik.gamedex.util.now
import io.ktor.client.call.*
import io.ktor.client.request.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:13
 */
@Singleton
open class IgdbClient @Inject constructor(
    private val config: IgdbConfig,
    @IgdbStorage private val storage: SingleValueStorage<IgdbStorageData>,
) {
    open suspend fun search(
        query: String,
        platform: Platform,
        account: IgdbUserAccount,
        offset: Int,
        limit: Int,
    ): List<SearchResult> = post(account) {
        // Split by non-word characters
        val queryWords = query.split("[\\W]".toRegex()).asSequence()
            .filter { it.isNotBlank() }
            .joinToString(" & ") { """name ~ *"$it"*""" }
        """
            search "$query";
            fields $searchFieldsStr;
            where $queryWords & release_dates.platform = ${platform.id};
            offset $offset;
            limit $limit;
        """.trimIndent()
    }

    open suspend fun fetch(providerGameId: String, account: IgdbUserAccount): FetchResult? =
        post<FetchResult>(account) {
            """
                fields $fetchDetailsFieldsStr;
                where id = $providerGameId;
            """.trimIndent()
        }.firstOrNull()

    private suspend inline fun <reified T : Any> post(account: IgdbUserAccount, body: () -> String): List<T> {
        val token = getAuthorizationToken(account)

        // Ktor fails to correctly parse a list of data classes, we have to do it manually.
        val response = try {
            httpClient.post(config.baseUrl) {
                header("Client-ID", account.clientId)
                header("Authorization", "Bearer $token")
                setBody(body())
            }.body<ByteArray>()
        } catch (e: Exception) {
            storage.reset()
            throw e
        }
        return response.listFromJson()
    }

    private suspend fun getAuthorizationToken(account: IgdbUserAccount): String {
        var data = storage.get()
        val now = now
        val expirationTime = data?.expiresOn?.minusSeconds(config.oauthTokenExpirationBufferSeconds)
        if (expirationTime == null || expirationTime.isBefore(now)) {
            val response = fetchAuthorization(account)
            val expiresOn = now.plusSeconds(response.expiresIn)
            data = IgdbStorageData(authorizationToken = response.accessToken, expiresOn)
            storage.set(data)
        }
        return data!!.authorizationToken
    }

    private suspend fun fetchAuthorization(account: IgdbUserAccount): OAuthResponse {
        return httpClient.post(config.oauthUrl) {
            parameter("client_id", account.clientId)
            parameter("client_secret", account.clientSecret)
            parameter("grant_type", "client_credentials")
        }.body()
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
            "release_dates.date",
            "release_dates.platform",
            "first_release_date",
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

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class SearchResult(
        val id: Int,
        override val name: String,
        override val summary: String?,
        override val aggregatedRating: Double?,
        override val aggregatedRatingCount: Int?,
        override val rating: Double?,
        override val ratingCount: Int?,
        override val releaseDates: List<ReleaseDate>?,
        override val firstReleaseDate: Long?,
        override val cover: Image?,
    ) : SharedSearchFetchFields

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ReleaseDate(
        val platform: Int,
        val date: Long?,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class FetchResult(
        val url: String,
        override val name: String,
        override val summary: String?,
        override val releaseDates: List<ReleaseDate>?,
        override val firstReleaseDate: Long?,
        override val aggregatedRating: Double?,
        override val aggregatedRatingCount: Int?,
        override val rating: Double?,
        override val ratingCount: Int?,
        override val cover: Image?,
        val screenshots: List<Image>?,
        val genres: List<Int>?,
    ) : SharedSearchFetchFields

    interface SharedSearchFetchFields {
        val name: String
        val summary: String?
        val aggregatedRating: Double?
        val aggregatedRatingCount: Int?
        val rating: Double?
        val ratingCount: Int?
        val releaseDates: List<ReleaseDate>?
        val firstReleaseDate: Long?
        val cover: Image?
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Image(
        val imageId: String?,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    internal data class OAuthResponse(
        val accessToken: String,
        val expiresIn: Int,
    )
}
