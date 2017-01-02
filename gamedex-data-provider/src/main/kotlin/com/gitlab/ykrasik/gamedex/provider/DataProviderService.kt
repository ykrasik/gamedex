package com.gitlab.ykrasik.gamedex.provider

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.*
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

        return GameDataDto(
            basicData = createBasicData(gameName),
            scoreData = createScoreData(gameName),
            imageData = createImageData(gameName),
            genresNames = this.flatMapTo(mutableSetOf<String>()) { it.genres }.toList(),
            providerData = this.map {
                ProviderData(
                    type = it.type,
                    detailUrl = it.detailUrl
                )
            }
        )
    }

    private fun List<ProviderGameData>.createBasicData(gameName: String): GameBasicData {
        val basicData = this.sortedBy { it.type.basicDataPriority }
        return GameBasicData(
            name = basicData.first().name,
            description = basicData.findFirst(gameName, "description") { it.description },
            releaseDate = basicData.findFirst(gameName, "releaseDate") { it.releaseDate }
        )
    }

    private fun List<ProviderGameData>.createScoreData(gameName: String): GameScoreData {
        val scoreData = this.sortedBy { it.type.scorePriority }
        return GameScoreData(
            criticScore = scoreData.findFirst(gameName, "criticScore") { it.criticScore },
            userScore = scoreData.findFirst(gameName, "userScore") { it.userScore }
        )
    }

    private fun List<ProviderGameData>.createImageData(gameName: String): GameImageData {
        val imageData = this.sortedBy { it.type.imagePriorty }
        return GameImageData(
            thumbnail = imageData.findFirst(gameName, "thumbnail") { it.thumbnailUrl?.fetchImage() } ?: ImageData.empty,
            poster = imageData.findFirst(gameName, "poster") { it.posterUrl?.fetchImage() } ?: ImageData.empty
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

    // FIXME: Add progress listeners.
    private fun String.fetchImage(): ImageData {
        // FIXME: Remove the try, only for debugging.
        try {
            // FIXME: Create a rest client api. Or use TornadoFX.
            val (request, response, result) = Fuel.download(this)
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
                is Result.Success -> ImageData(result.value, this)
            }
            log.info { "Done. Size: ${imageData.rawData!!.size}" }
            return imageData
        } catch (e: Exception) {
            throw e
        }
    }
}