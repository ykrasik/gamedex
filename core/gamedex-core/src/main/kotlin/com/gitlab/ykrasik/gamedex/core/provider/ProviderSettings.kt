/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.core.settings.SettingsRepo
import com.gitlab.ykrasik.gamedex.core.settings.UserSettings
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderOrderPriorities
import com.gitlab.ykrasik.gamedex.util.Extractor
import com.gitlab.ykrasik.gamedex.util.Modifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 23/04/2017
 * Time: 12:48
 */
@Singleton
class ProviderSettings @Inject constructor(private val providers: MutableSet<GameProvider>) : UserSettings() {
    override val repo = SettingsRepo("provider") {
        val defaultSearchOrder = byPriority { search }
        val defaultNameOrder = byPriority { name }
        val defaultDescriptionOrder = byPriority { description }
        val defaultReleaseDateOrder = byPriority { releaseDate }
        val defaultCriticScoreOrder = byPriority { criticScore }
        val defaultUserScoreOrder = byPriority { userScore }
        val defaultThumbnailOrder = byPriority { thumbnail }
        val defaultPosterOrder = byPriority { poster }
        val defaultScreenshotOrder = byPriority { screenshot }
        Data(
            providers = providers.map { provider ->
                provider.id to ProviderUserSettings(
                    enable = provider.accountFeature == null,
                    account = null,
                    order = ProviderOrderPriorities(
                        search = defaultSearchOrder[provider.id]!!,
                        name = defaultNameOrder[provider.id]!!,
                        description = defaultDescriptionOrder[provider.id]!!,
                        releaseDate = defaultReleaseDateOrder[provider.id]!!,
                        criticScore = defaultCriticScoreOrder[provider.id]!!,
                        userScore = defaultUserScoreOrder[provider.id]!!,
                        thumbnail = defaultThumbnailOrder[provider.id]!!,
                        poster = defaultPosterOrder[provider.id]!!,
                        screenshot = defaultScreenshotOrder[provider.id]!!
                    )
                )
            }.toMap()
        )
    }

    private fun byPriority(extractor: Extractor<ProviderOrderPriorities, Int>) =
        providers.sortedBy { extractor(it.defaultOrder) }.mapIndexed { i, provider -> provider.id to i }.toMap()

    val providerSettingsSubject = repo.subject(Data::providers) { copy(providers = it) }
    var providerSettings by providerSettingsSubject

    val searchOrderSubject = orderSubject(ProviderOrderPriorities::search) { copy(search = it) }
    var searchOrder by searchOrderSubject

    val nameOrderSubject = orderSubject(ProviderOrderPriorities::name) { copy(name = it) }
    var nameOrder by nameOrderSubject

    val descriptionOrderSubject = orderSubject(ProviderOrderPriorities::description) { copy(description = it) }
    var descriptionOrder by descriptionOrderSubject

    val releaseDateOrderSubject = orderSubject(ProviderOrderPriorities::releaseDate) { copy(releaseDate = it) }
    var releaseDateOrder by releaseDateOrderSubject

    val criticScoreOrderSubject = orderSubject(ProviderOrderPriorities::criticScore) { copy(criticScore = it) }
    var criticScoreOrder by criticScoreOrderSubject

    val userScoreOrderSubject = orderSubject(ProviderOrderPriorities::userScore) { copy(userScore = it) }
    var userScoreOrder by userScoreOrderSubject

    val thumbnailOrderSubject = orderSubject(ProviderOrderPriorities::thumbnail) { copy(thumbnail = it) }
    var thumbnailOrder by thumbnailOrderSubject

    val posterOrderSubject = orderSubject(ProviderOrderPriorities::poster) { copy(poster = it) }
    var posterOrder by posterOrderSubject

    val screenshotOrderSubject = orderSubject(ProviderOrderPriorities::screenshot) { copy(screenshot = it) }
    var screenshotOrder by screenshotOrderSubject

    private fun orderSubject(extractor: Extractor<ProviderOrderPriorities, Int>,
                             modifier: Modifier<ProviderOrderPriorities, Int>) =
        repo.subject({ order(extractor) }) { fromOrder(it, modifier) }

    private inline fun Data.order(extractor: Extractor<ProviderOrderPriorities, Int>): Order = Order(
        providers.mapValues { (_, settings) -> extractor(settings.order) }
    )

    private inline fun Data.fromOrder(order: Order, crossinline modifier: Modifier<ProviderOrderPriorities, Int>): Data = copy(
        providers = providers.mapValues { (providerId, settings) -> settings.withOrder { modifier(order[providerId]) } }
    )

    private fun ProviderUserSettings.withOrder(modifier: ProviderOrderPriorities.() -> ProviderOrderPriorities) = copy(order = modifier(order))

    operator fun get(providerId: ProviderId) = providerSettings[providerId]!!
    operator fun set(providerId: ProviderId, settings: ProviderUserSettings) {
        providerSettings += providerId to settings
    }

    fun modify(providerId: ProviderId, f: ProviderUserSettings.() -> ProviderUserSettings) {
        providerSettings += providerId to f(get(providerId))
    }

    data class Order(private val initialPriorities: Map<ProviderId, Int>) {
        private val order = initialPriorities.toList().sortedBy { it.second }.mapIndexed { i, (providerId, _) ->
            providerId to i
        }.toMap()

        operator fun get(id: ProviderId) = order[id]!!

        fun <T : GameProvider> toComparator(): Comparator<T> = Comparator { o1, o2 -> get(o1.id).compareTo(get(o2.id)) }

        fun ordered() = order.entries.sortedBy { it.value }.map { it.key }

        fun switch(a: ProviderId, b: ProviderId): Order {
            val currentA = order[a]!!
            val currentB = order[b]!!
            return Order(order + (a to currentB) + (b to currentA))
        }

        companion object {
            val minOrder = -1
        }
    }

    data class Data(
        val providers: Map<ProviderId, ProviderUserSettings>
    )
}

data class ProviderUserSettings(
    val enable: Boolean,
    val account: Map<String, String>?,
    val order: ProviderOrderPriorities
)