package com.gitlab.ykrasik.gamedex.provider.giantbomb

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.gitlab.ykrasik.gamedex.common.util.EnumIdConverter
import com.gitlab.ykrasik.gamedex.common.util.IdentifiableEnum
import org.joda.time.LocalDate

/**
 * User: ykrasik
 * Date: 29/01/2017
 * Time: 11:30
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombSearchResponse(
    val statusCode: GiantBombStatus,
    val results: List<GiantBombSearchResult>
)

data class GiantBombSearchResult(
    val apiDetailUrl: String,
    val name: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    val originalReleaseDate: LocalDate?,
    val image: GiantBombSearchImage
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombSearchImage(
    val thumbUrl: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombDetailsResponse(
    val statusCode: GiantBombStatus,

    // When result is found - GiantBomb returns a Json object. When result is not found, GiantBomb returns an empty Json array []. Annoying.
    @JsonFormat(with = arrayOf(JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY))
    val results: List<GiantBombDetailsResult>
)

data class GiantBombDetailsResult(
    val siteDetailUrl: String,
    val deck: String?,
    val image: GiantBombDetailsImage,
    val genres: List<GiantBombGenre>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombGenre(
    val name: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GiantBombDetailsImage(
    val thumbUrl: String,
    val superUrl: String
)

enum class GiantBombStatus constructor(override val key: Int) : IdentifiableEnum<Int> {
    ok(1),
    invalidApiKey(100),
    notFound(101),
    badFormat(102),
    jsonPNoCallback(103),
    filterError(104),
    videoOnlyForSubscribers(105);

    override fun toString() = "$name($key)"

    companion object {
        private val values = EnumIdConverter(GiantBombStatus::class.java)

        @JsonCreator
        @JvmStatic
        operator fun invoke(code: Int): GiantBombStatus = values[code]
    }
}