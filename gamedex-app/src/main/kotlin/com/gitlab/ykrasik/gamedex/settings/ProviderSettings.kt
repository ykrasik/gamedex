package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.GameProviderType
import tornadofx.getValue
import tornadofx.setValue
import java.util.*
import kotlin.Comparator

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 12:48
 */
class ProviderSettings private constructor() : AbstractSettings("provider") {
    companion object {
        operator fun invoke(): ProviderSettings = readOrUse(ProviderSettings())
    }

    @Transient
    val searchOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var searchOrder by searchOrderProperty

    @Transient
    val nameOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var nameOrder by nameOrderProperty

    @Transient
    val descriptionOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var descriptionOrder by descriptionOrderProperty

    @Transient
    val releaseDateOrderProperty = preferenceProperty(Order.prefer(GameProviderType.GiantBomb))
    var releaseDateOrder by releaseDateOrderProperty

    @Transient
    val criticScoreOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var criticScoreOrder by criticScoreOrderProperty

    @Transient
    val userScoreOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var userScoreOrder by userScoreOrderProperty

    @Transient
    val thumbnailOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var thumbnailOrder by thumbnailOrderProperty

    @Transient
    val posterOrderProperty = preferenceProperty(Order.prefer(GameProviderType.GiantBomb))
    var posterOrder by posterOrderProperty

    @Transient
    val screenshotOrderProperty = preferenceProperty(Order.prefer(GameProviderType.Igdb))
    var screenshotOrder by screenshotOrderProperty

    data class Order(val priorities: EnumMap<GameProviderType, Int>) {
        fun preferredProvider(): GameProviderType = priorities.maxBy { it.value }!!.key

        operator fun get(type: GameProviderType) = priorities[type]!!
        operator fun get(provider: GameProvider) = get(provider.type)

        fun toComparator(): Comparator<GameProvider> = Comparator { o1, o2 -> get(o1).compareTo(get(o2)) }

        companion object {
            operator fun invoke(priorities: Map<GameProviderType, Int>): Order = Order(EnumMap(priorities))

            fun prefer(type: GameProviderType): Order = invoke(
                // Make sure preferred type has highest priority
                GameProviderType.values().associate { it to it.ordinal } + (type to maxPriority)
            )

            val maxPriority = GameProviderType.values().size
        }
    }
}