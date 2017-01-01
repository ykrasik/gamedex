package com.gitlab.ykrasik.gamedex.provider

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.firstNotNull
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GameDataDto
import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.github.ykrasik.gamedex.datamodel.ProviderData
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 13/10/2016
 * Time: 13:29
 */
interface DataProviderService {
    fun fetch(name: String, platform: GamePlatform): GameDataDto?
}

@Singleton
class DataProviderServiceImpl @Inject constructor(
    private val providers: MutableSet<DataProvider>,
    private val chooser: GameSearchChooser
) : DataProviderService {
    private val log by logger()

    override fun fetch(name: String, platform: GamePlatform): GameDataDto? {
        check(providers.isNotEmpty()) { "No providers are active! Please activate at least 1 provider." }

        val context = SearchContext()
        val providerGameData = providers.map { provider ->
            val results = provider.search(name, platform)
            val result = chooser.choose(results, context) ?: return null
            provider.fetch(result)
        }
        return providerGameData.merge(name)
    }

    private fun List<ProviderGameData>.merge(gameName: String): GameDataDto {
        check(this.isNotEmpty()) { "No provider managed to return any GameData!" }

        val basicData = this.sortedBy { it.type.basicDataPriority }
        val scoreData = this.sortedBy { it.type.scorePriority }
        val imageData = this.sortedBy { it.type.imagePriorty }

        return GameDataDto(
            name = basicData.first().name,
            description = basicData.findFirst(gameName, "description") { it.description },
            releaseDate = basicData.findFirst(gameName, "releaseDate") { it.releaseDate },

            criticScore = scoreData.findFirst(gameName, "criticScore") { it.criticScore },
            userScore = scoreData.findFirst(gameName, "userScore") { it.userScore },

            thumbnail = imageData.firstNotNull { it.fetchThumbnail() },
            poster = imageData.firstNotNull { it.fetchPoster() },

            genres = this.flatMapTo(mutableSetOf<String>()) { it.genres }.toList(),

            providerData = this.map {
                ProviderData(
                    type = it.type,
                    detailUrl = it.detailUrl
                )
            }
        )
    }

    private fun <T> List<ProviderGameData>.findFirst(gameName: String, field: String, extractor: (ProviderGameData) -> T?): T? {
        val providerData = this.firstOrNull { extractor(it) != null }
        return if (providerData != null) {
            val value = extractor(providerData)
            log.debug { "[$gameName][$field][${providerData.type}]: $value" }
            value
        } else {
            log.debug { "[$gameName][$field]: Empty." }
            null
        }
    }

    private fun ProviderGameData.fetchThumbnail(): ImageData? = thumbnailUrl?.let {
        log.debug { "[$name][$type] Fetching thumbnail: '$it'..." }
        fetchImage(it)
    }

    private fun ProviderGameData.fetchPoster(): ImageData? = posterUrl?.let {
        log.debug { "[$name][$type] Fetching poster: '$it'..." }
        fetchImage(it)
    }

    // FIXME: Add progress listeners.
    private fun fetchImage(url: String): ImageData? {
        try {
            val (request, response, result) = Fuel.download(url)
                .destination { response, url ->
                    File.createTempFile("temp", ".tmp")
                }
                .header("User-Agent" to "Kotlin-Fuel")
                .progress { readBytes, totalBytes ->
                    val progress = readBytes.toFloat() / totalBytes.toFloat()
                    log.info { "Progress: $progress" }
                }.response()

            val imageData = when (result) {
                is Result.Failure -> throw result.error
                is Result.Success -> ImageData(result.value)
            }
            log.info { "Done. Size: ${imageData.rawData.size}" }
            return imageData
        } catch (e: Exception) {
            throw e
        }
    }
}