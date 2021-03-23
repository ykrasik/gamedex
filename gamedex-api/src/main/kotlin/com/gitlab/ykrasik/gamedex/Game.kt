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

package com.gitlab.ykrasik.gamedex

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.file
import com.gitlab.ykrasik.gamedex.util.firstNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.joda.time.DateTime
import java.io.File

/**
 * User: ykrasik
 * Date: 26/05/2016
 * Time: 15:32
 */
typealias GameId = Int

data class Game(
    val rawGame: RawGame,
    val library: Library,
    val gameData: GameData,
    val folderName: FolderName,
    val fileTree: StateFlow<FileTree?>,
    val genres: List<Genre>,
    val filterTags: List<TagId>,
) {
    val id get() = rawGame.id
    val path by lazy { library.path.resolve(metadata.path) }
    val createDate get() = metadata.createDate
    val updateDate get() = metadata.updateDate
    val userData get() = rawGame.userData
    val metadata get() = rawGame.metadata

    val platform get() = library.platform

    val name get() = gameData.name
    val description get() = gameData.description
    val releaseDate get() = gameData.releaseDate
    val criticScore get() = gameData.criticScore
    val userScore get() = gameData.userScore
    val minScore get() = withBothScores { c, u -> minOf(c, u) }
    val maxScore get() = withBothScores { c, u -> maxOf(c, u) }
    val avgScore get() = withBothScores { c, u -> (c + u) / 2 }
    private inline fun withBothScores(f: (criticScore: Double, userScore: Double) -> Double): Double? =
        criticScore?.let { c -> userScore?.let { u -> f(c.score, u.score) } }

    val thumbnailUrl get() = gameData.thumbnailUrl ?: gameData.posterUrl
    val posterUrl get() = gameData.posterUrl ?: gameData.thumbnailUrl
    val screenshotUrls get() = gameData.screenshotUrls

    val tags get() = userData.tags

    val providerData get() = rawGame.providerData
    val providerHeaders get() = providerData.asSequence().map { it.header }
    val existingProviders get() = providerHeaders.map { it.providerId }
    val excludedProviders get() = userData.excludedProviders

    val mainExecutableFile get() = userData.mainExecutablePath?.let { path.resolve(it) }
    val absoluteFileTree get() = fileTree.value?.copy(name = path.absolutePath)

    fun isProviderExcluded(providerId: ProviderId) = excludedProviders.contains(providerId)

    override fun toString() = "[$platform] Game(id = $id, name = '$name', path = $path)"

    companion object {
        val Null = Game(
            rawGame = RawGame(
                id = 0,
                metadata = Metadata(
                    libraryId = 0,
                    path = "",
                    timestamp = Timestamp.Null
                ),
                providerData = emptyList(),
                userData = UserData.Null
            ),
            library = Library.Null,
            gameData = GameData(
                name = "",
                description = null,
                releaseDate = null,
                criticScore = null,
                userScore = null,
                genres = emptyList(),
                thumbnailUrl = null,
                posterUrl = null,
                screenshotUrls = emptyList()
            ),
            folderName = FolderName(
                rawName = "",
                processedName = "",
                order = null,
                metaTag = null,
                version = null
            ),
            fileTree = MutableStateFlow(null),
            genres = emptyList(),
            filterTags = emptyList()
        )
    }
}

data class RawGame(
    val id: GameId,
    val metadata: Metadata,
    val providerData: List<ProviderData>,
    val userData: UserData,
) {
    fun withMetadata(metadata: Metadata) = copy(metadata = metadata)
    fun withMetadata(f: (Metadata) -> Metadata) = withMetadata(f(metadata))
}

data class ProviderData(
    val header: ProviderHeader,
    val gameData: GameData,
    val siteUrl: String,
    val timestamp: Timestamp,
) {
    val providerId get() = header.providerId
    val providerGameId get() = header.providerGameId

    fun updatedNow() = copy(timestamp = timestamp.updatedNow())
}

data class GameData(
    val name: String,
    val description: String?,
    val releaseDate: String?,
    val criticScore: Score?,
    val userScore: Score?,
    val genres: List<String>,
    val thumbnailUrl: String?,
    val posterUrl: String?,
    val screenshotUrls: List<String>,
)

data class Score(
    val score: Double,
    val numReviews: Int,
) : Comparable<Score> {
    override fun compareTo(other: Score) = score.compareTo(other.score)
}

data class ProviderHeader(
    val providerId: ProviderId,
    val providerGameId: String,
)

data class Metadata(
    val libraryId: LibraryId,
    val path: String,
    val timestamp: Timestamp,
) {
    val createDate get() = timestamp.createDate
    val updateDate get() = timestamp.updateDate
    fun withCreateDate(createDate: DateTime) = copy(timestamp = timestamp.withCreateDate(createDate))
    fun createdNow() = copy(timestamp = timestamp.createdNow())
    fun updatedNow() = copy(timestamp = timestamp.updatedNow())
}

data class Timestamp(
    val createDate: DateTime,
    val updateDate: DateTime,
) {
    fun withCreateDate(createDate: DateTime) = copy(createDate = createDate)
    fun createdNow() = com.gitlab.ykrasik.gamedex.util.now.let { copy(createDate = it, updateDate = it) }
    fun updatedNow() = copy(updateDate = com.gitlab.ykrasik.gamedex.util.now)

    companion object {
        val Null = Timestamp(DateTime(0), DateTime(0))
        val now get() = com.gitlab.ykrasik.gamedex.util.now.let { now -> Timestamp(now, now) }
    }
}

data class FileTree(
    val name: String,
    val size: FileSize,
    val isDirectory: Boolean,
    val children: List<FileTree>,
) {
    fun pathTo(target: FileTree): File? =
        if (this === target) {
            name.file
        } else {
            children.asSequence()
                .mapNotNull { it.pathTo(target) }
                .map { name.file.resolve(it) }
                .firstNotNull()
        }

    fun find(target: File): FileTree? {
        if (target.toString() == name) return this
        val leftover = target.relativeToOrNull(name.file) ?: return null
        return children.asSequence()
            .mapNotNull { it.find(leftover) }
            .firstNotNull()
    }

    companion object {
        val Null = FileTree(
            name = "Not Available",
            size = FileSize.Empty,
            isDirectory = true,
            children = emptyList()
        )
    }
}

data class FolderName(
    val rawName: String,
    val processedName: String,
    val order: String?,            // TODO: Consider adding option to display this.
    val metaTag: String?,
    val version: String?,
) {
    fun isSameBaseName(other: FolderName): Boolean {
        return processedName == other.processedName
    }
}

typealias TagId = String

enum class GameDataType(val displayName: String) {
    Name("Name"),
    Description("Description"),
    ReleaseDate("Release Date"),
    CriticScore("Critic Score"),
    UserScore("User Score"),
    Genres("Genres"),
    Thumbnail("Thumbnail"),
    Poster("Poster"),
    Screenshots("Screenshots")
}

data class UserData(
    val overrides: Map<GameDataType, GameDataOverride> = emptyMap(),
    val tags: List<TagId> = emptyList(),
    val excludedProviders: List<ProviderId> = emptyList(),
    val mainExecutablePath: String? = null,
) {
    fun nameOverride() = overrides[GameDataType.Name]
    fun descriptionOverride() = overrides[GameDataType.Description]
    fun releaseDateOverride() = overrides[GameDataType.ReleaseDate]
    fun criticScoreOverride() = overrides[GameDataType.CriticScore]
    fun userScoreOverride() = overrides[GameDataType.UserScore]
    fun genresOverride() = overrides[GameDataType.Genres]
    fun thumbnailOverride() = overrides[GameDataType.Thumbnail]
    fun posterOverride() = overrides[GameDataType.Poster]
    fun screenshotsOverride() = overrides[GameDataType.Screenshots]

    companion object {
        val Null = UserData()
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = GameDataOverride.Provider::class, name = "provider"),
    JsonSubTypes.Type(value = GameDataOverride.Custom::class, name = "custom")
)
sealed class GameDataOverride {
    data class Provider(val provider: ProviderId) : GameDataOverride()
    data class Custom(val value: Any) : GameDataOverride()
}