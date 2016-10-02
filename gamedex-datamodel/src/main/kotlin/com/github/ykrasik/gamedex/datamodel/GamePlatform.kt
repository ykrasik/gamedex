package com.github.ykrasik.gamedex.datamodel

import com.github.ykrasik.gamedex.common.enums.EnumIdConverter
import com.github.ykrasik.gamedex.common.enums.IdentifiableEnum

/**
 * @author Yevgeny Krasik
 */
enum class GamePlatform constructor(private val key: String) : IdentifiableEnum<String> {
    PC("PC"),
    XBOX_360("Xbox 360"),
    XBOX_ONE("Xbox One"),
    PS3("PlayStation 3"),
    PS4("PlayStation 4");

    override fun getKey() = key

    override fun toString() = key

    companion object {
        private val values = EnumIdConverter(GamePlatform::class.java)

        operator fun invoke(name: String): GamePlatform = values.get(name)
    }
}
