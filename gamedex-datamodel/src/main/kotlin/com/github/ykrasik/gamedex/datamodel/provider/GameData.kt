package com.github.ykrasik.gamedex.datamodel.provider

import com.github.ykrasik.gamedex.datamodel.ImageData
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 27/05/2016
 * Time: 23:16
 */
// FIXME: Something needs to be done about this class.
data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,

    val criticScore: Double?,
    val userScore: Double?,

    val thumbnail: ImageData?,
    val poster: ImageData?,

    val genres: List<String>,

    val metacriticUrl: String,
    val giantBombUrl: String?
) {

//    companion object {
//        operator fun invoke(metacriticData: com.gitlab.ykrasik.gamedex.provider.ProviderGameData, giantBombData: com.gitlab.ykrasik.gamedex.provider.ProviderGameData?): GameData {
//            val thumbnail = giantBombData?.thumbnail ?: metacriticData.thumbnail
//            return GameData(
//                name = metacriticData.name,
//                description = giantBombData?.description ?: metacriticData.description,
//                releaseDate = metacriticData.releaseDate ?: giantBombData?.releaseDate,
//                criticScore = metacriticData.criticScore,
//                userScore = metacriticData.userScore,
//                thumbnail = thumbnail,
//                poster = giantBombData?.poster ?: thumbnail,
//                genres = giantBombData?.genres ?: metacriticData.genres,
//                metacriticUrl = metacriticData.detailUrl,
//                giantBombUrl = giantBombData?.detailUrl
//            )
//        }
//    }
}