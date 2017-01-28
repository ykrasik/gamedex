package com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson

import com.fasterxml.jackson.annotation.JsonCreator
import com.github.ykrasik.gamedex.common.util.EnumIdConverter
import com.github.ykrasik.gamedex.common.util.IdentifiableEnum

/**
 * User: ykrasik
 * Date: 01/10/2016
 * Time: 10:58
 */
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