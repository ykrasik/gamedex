package com.gitlab.ykrasik.gamedex

import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.io.File

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
data class Game(
    val id: Int,
    private val metaData: MetaData,
    private val gameData: GameData,
    val providerData: List<ProviderData>,
    val imageUrls: ImageUrls
) {
    val libraryId get() = metaData.libraryId
    val path get() = metaData.path
    val lastModified get() = metaData.lastModified

    val name get() = gameData.name
    val description get() = gameData.description
    val releaseDate get() = gameData.releaseDate

    val criticScore get() = gameData.criticScore
    val userScore get() = gameData.userScore

    val genres get() = gameData.genres

    override fun toString() = "Game(id = $id, name = $name, path = $path)"
}

data class RawGame(
    val id: Int,
    val metaData: MetaData,
    val providerData: List<ProviderFetchResult>
)

data class MetaData(
    val libraryId: Int,
    val path: File,
    val lastModified: DateTime
)

data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: LocalDate?,

    val criticScore: Double?,
    val userScore: Double?,

    val genres: List<String>
)

data class ProviderData(
    val type: DataProviderType,
    val apiUrl: String,
    val siteUrl: String
)

data class ImageUrls(
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>
)