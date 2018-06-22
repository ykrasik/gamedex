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

package com.gitlab.ykrasik.gamedex.app.api.settings

import com.gitlab.ykrasik.gamedex.app.api.image.Image
import com.gitlab.ykrasik.gamedex.provider.GameProvider
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.experimental.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 09:25
 */
interface ProviderOrderSettingsView {
    // TODO: Consider just allowing the view to receive this data through a CommonData object
    var providerLogos: Map<ProviderId, Image>

    var search: Order
    val searchChanges: ReceiveChannel<Order>

    var name: Order
    val nameChanges: ReceiveChannel<Order>

    var description: Order
    val descriptionChanges: ReceiveChannel<Order>

    var releaseDate: Order
    val releaseDateChanges: ReceiveChannel<Order>

    var criticScore: Order
    val criticScoreChanges: ReceiveChannel<Order>

    var userScore: Order
    val userScoreChanges: ReceiveChannel<Order>

    var thumbnail: Order
    val thumbnailChanges: ReceiveChannel<Order>

    var poster: Order
    val posterChanges: ReceiveChannel<Order>

    var screenshot: Order
    val screenshotChanges: ReceiveChannel<Order>
}

class Order(private val order: Map<ProviderId, Int>) {
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