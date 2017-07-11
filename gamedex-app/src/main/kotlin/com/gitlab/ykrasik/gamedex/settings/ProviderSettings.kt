package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.GameProvider
import com.gitlab.ykrasik.gamedex.ProviderId
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 12:48
 */
class ProviderSettings private constructor() : Settings("provider") {
    companion object {
        operator fun invoke(): ProviderSettings = readOrUse(ProviderSettings())

        // TODO: Does this mean that each provider needs to declare these fields?
        private val Igdb = "Igdb"
        private val GiantBomb = "GiantBomb"
    }

    @Transient
    val searchOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var searchOrder by searchOrderProperty

    @Transient
    val nameOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var nameOrder by nameOrderProperty

    @Transient
    val descriptionOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var descriptionOrder by descriptionOrderProperty

    @Transient
    val releaseDateOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var releaseDateOrder by releaseDateOrderProperty

    @Transient
    val criticScoreOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var criticScoreOrder by criticScoreOrderProperty

    @Transient
    val userScoreOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var userScoreOrder by userScoreOrderProperty

    @Transient
    val thumbnailOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var thumbnailOrder by thumbnailOrderProperty

    @Transient
    val posterOrderProperty = preferenceProperty(Order(GiantBomb, Igdb))
    var posterOrder by posterOrderProperty

    @Transient
    val screenshotOrderProperty = preferenceProperty(Order(Igdb, GiantBomb))
    var screenshotOrder by screenshotOrderProperty

    data class Order(val order: Map<ProviderId, Int>) {
        operator fun get(id: ProviderId) = order[id]!!

        fun toComparator(): Comparator<GameProvider> = Comparator { o1, o2 -> get(o1.id).compareTo(get(o2.id)) }

        fun ordered() = order.entries.sortedBy { it.value }.map { it.key }

        fun switch(a: ProviderId, b: ProviderId): Order {
            val currentA = order[a]!!
            val currentB = order[b]!!
            return Order(order + (a to currentB) + (b to currentA))
        }

        companion object {
            val minOrder = -1

            operator fun invoke(vararg order: ProviderId): Order {
                require(order.distinct() == order.toList()) { "Providers may only appear once in order!" }
                return Order(order.mapIndexed { i, o -> o to i }.toMap())
            }
        }
    }
}