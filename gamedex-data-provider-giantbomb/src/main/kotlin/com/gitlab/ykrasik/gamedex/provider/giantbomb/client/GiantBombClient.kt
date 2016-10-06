package com.gitlab.ykrasik.gamedex.provider.giantbomb.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.provider.giantbomb.GiantBombConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 29/05/2016
 * Time: 10:43
 */
@Singleton
class GiantBombClient @Inject constructor(
    private val config: GiantBombConfig,
    private val mapper: ObjectMapper
) {
    private val log by logger()

    private val searchFields = listOf("api_detail_url", "name", "original_release_date", "image").joinToString(",")
    private val fetchDetailsFields = listOf("name", "deck", "original_release_date", "image", "genres").joinToString(",")

    fun search(name: String, platform: GamePlatform): GiantBombSearchResponse {
        val platformId = config.getPlatformId(platform)
        val (request, response, result) = getRequest("http://www.giantbomb.com/api/games",
            "filter" to "name:$name,platforms:$platformId",
            "field_list" to searchFields
        )
        return result.fromJson(GiantBombSearchResponse::class.java)
    }

    fun fetch(detailUrl: String): GiantBombDetailsResponse = try {
        val (request, response, result) = getRequest(detailUrl, "field_list" to fetchDetailsFields)
        result.fromJson(GiantBombDetailsResponse::class.java)
    } catch (e: FuelError) {
        e.exception.let {
            when (it) {
                is HttpException -> if (it.httpCode == 404) {
                    GiantBombDetailsResponse(statusCode = GiantBombStatus.notFound, results = emptyList())
                } else {
                    throw e
                }
                else -> throw e
            }
        }
    }

    private fun getRequest(path: String, vararg elements: Pair<String, Any?>) = path.httpGet(params(elements))
        .header("User-Agent" to "Kotlin-Fuel")
        .response()

    private fun params(elements: Array<out Pair<String, Any?>>): List<Pair<String, Any?>> = mutableListOf(
        "api_key" to config.applicationKey,
        "format" to "json"
    ) + elements

    private fun <T> Result<ByteArray, FuelError>.fromJson(clazz: Class<T>): T = when (this) {
        is Result.Failure -> throw error
        is Result.Success -> {
            log.debug { "Raw result: ${String(value)}" }
            mapper.readValue(value, clazz)
        }
    }
}