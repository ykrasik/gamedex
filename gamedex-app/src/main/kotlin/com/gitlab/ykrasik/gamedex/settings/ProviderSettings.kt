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
// TODO: Support full ordering between providers via drag & drop.
class ProviderSettings private constructor() : AbstractSettings("provider") {
    companion object {
        operator fun invoke(): ProviderSettings = readOrUse(ProviderSettings())
    }

    @Transient
    val searchOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var searchOrder by searchOrderProperty

    @Transient
    val nameOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var nameOrder by nameOrderProperty

    @Transient
    val descriptionOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var descriptionOrder by descriptionOrderProperty

    @Transient
    val releaseDateOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var releaseDateOrder by releaseDateOrderProperty

    @Transient
    val criticScoreOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var criticScoreOrder by criticScoreOrderProperty

    @Transient
    val userScoreOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var userScoreOrder by userScoreOrderProperty

    @Transient
    val thumbnailOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var thumbnailOrder by thumbnailOrderProperty

    @Transient
    val posterOrderProperty = preferenceProperty(Order(GameProviderType.GiantBomb, GameProviderType.Igdb))
    var posterOrder by posterOrderProperty

    @Transient
    val screenshotOrderProperty = preferenceProperty(Order(GameProviderType.Igdb, GameProviderType.GiantBomb))
    var screenshotOrder by screenshotOrderProperty

    data class Order(val order: EnumMap<GameProviderType, Int>) {
        operator fun get(type: GameProviderType) = order[type]!!
        operator fun get(provider: GameProvider) = get(provider.type)

        fun toComparator(): Comparator<GameProvider> = Comparator { o1, o2 -> get(o1).compareTo(get(o2)) }

        fun ordered() = order.entries.sortedBy { it.value }.map { it.key }

        fun switch(a: GameProviderType, b: GameProviderType): Order {
            val currentA = order[a]!!
            val currentB = order[b]!!
            return Order(order + (a to currentB) + (b to currentA))
        }

        companion object {
            val minOrder = -1

            operator fun invoke(priorities: Map<GameProviderType, Int>): Order = Order(EnumMap(priorities))
            operator fun invoke(vararg order: GameProviderType): Order {
                require(order.size == GameProviderType.values().size) { "Missing a provider for order!" }
                require(order.distinct() == order.toList()) { "Providers may only appear once in order!" }
                return invoke(order.mapIndexed { i, o -> o to i }.toMap())
            }
        }
    }
}