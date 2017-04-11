package com.gitlab.ykrasik.gamedex.datamodel

import com.gitlab.ykrasik.gamedex.common.util.EnumIdConverter
import com.gitlab.ykrasik.gamedex.common.util.IdentifiableEnum

/**
 * @author Yevgeny Krasik
 */
enum class GamePlatform constructor(override val key: String) : IdentifiableEnum<String> {
    pc("PC"),
    xbox360("Xbox 360"),
    xboxOne("Xbox One"),
    ps3("PlayStation 3"),
    ps4("PlayStation 4"),
    excluded("Excluded");

    override fun toString() = key

    companion object {
        private val values = EnumIdConverter(GamePlatform::class.java)

        operator fun invoke(name: String): GamePlatform = values[name]
    }
}
