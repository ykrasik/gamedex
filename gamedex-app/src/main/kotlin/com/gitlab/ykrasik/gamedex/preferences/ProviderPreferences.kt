package com.gitlab.ykrasik.gamedex.preferences

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
class ProviderPreferences private constructor() : UserPreferencesSet("provider") {
    companion object {
        operator fun invoke(): ProviderPreferences = readOrUse(ProviderPreferences())
    }

    @Transient
    val searchOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.GiantBomb))
    var searchOrder by searchOrderProperty

    @Transient
    val nameOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.GiantBomb))
    var nameOrder by nameOrderProperty

    @Transient
    val descriptionOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.Igdb))
    var descriptionOrder by descriptionOrderProperty

    @Transient
    val releaseDateOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.GiantBomb))
    var releaseDateOrder by releaseDateOrderProperty

    @Transient
    val criticScoreOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.Igdb))
    var criticScoreOrder by criticScoreOrderProperty

    @Transient
    val userScoreOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.Igdb))
    var userScoreOrder by userScoreOrderProperty

    @Transient
    val thumbnailOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.Igdb))
    var thumbnailOrder by thumbnailOrderProperty

    @Transient
    val posterOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.GiantBomb))
    var posterOrder by posterOrderProperty

    @Transient
    val screenshotOrderProperty = preferenceProperty(DefaultProviderOrder.prefer(GameProviderType.Igdb))
    var screenshotOrder by screenshotOrderProperty
}

data class DefaultProviderOrder(val priorities: EnumMap<GameProviderType, Int>) {
    fun preferredProvider(): GameProviderType = priorities.maxBy { it.value }!!.key

    operator fun get(type: GameProviderType) = priorities[type]!!
    operator fun get(provider: GameProvider) = get(provider.info.type)

    fun toComparator(): Comparator<GameProvider> = Comparator { o1, o2 -> get(o1).compareTo(get(o2)) }

    companion object {
        operator fun invoke(priorities: Map<GameProviderType, Int>): DefaultProviderOrder = DefaultProviderOrder(EnumMap(priorities))

        fun prefer(type: GameProviderType): DefaultProviderOrder = invoke(
            // Make sure preferred type has highest priority
            GameProviderType.values().associate { it to it.ordinal } + (type to maxPriority)
        )

        val maxPriority = GameProviderType.values().size
    }
}