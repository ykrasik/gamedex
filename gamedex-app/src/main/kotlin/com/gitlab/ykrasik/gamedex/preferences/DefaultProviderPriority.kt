package com.gitlab.ykrasik.gamedex.preferences

import com.gitlab.ykrasik.gamedex.GameProviderType
import java.util.*

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 12:48
 */
data class DefaultProviderPriority(val priorities: EnumMap<GameProviderType, Int>) {
    fun preferredProvider(): GameProviderType = priorities.maxBy { it.value }!!.key

    operator fun get(type: GameProviderType) = priorities[type]

    companion object {
        operator fun invoke(priorities: Map<GameProviderType, Int>): DefaultProviderPriority = DefaultProviderPriority(EnumMap(priorities))

        fun prefer(type: GameProviderType): DefaultProviderPriority = invoke(
            // Make sure preferred type has highest priority
            GameProviderType.values().associate { it to it.ordinal } + (type to maxPriority)
        )

        val maxPriority = GameProviderType.values().size
    }
}
