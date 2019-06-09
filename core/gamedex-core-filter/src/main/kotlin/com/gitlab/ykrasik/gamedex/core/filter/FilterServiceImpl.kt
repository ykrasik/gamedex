/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.filter

import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.TagId
import com.gitlab.ykrasik.gamedex.app.api.filter.*
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.provider.GameProviderService
import com.gitlab.ykrasik.gamedex.core.task.task
import com.gitlab.ykrasik.gamedex.core.util.broadcastTo
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.id
import com.gitlab.ykrasik.gamedex.provider.supports
import com.gitlab.ykrasik.gamedex.util.logger
import com.gitlab.ykrasik.gamedex.util.months
import com.gitlab.ykrasik.gamedex.util.time
import com.google.inject.BindingAnnotation
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 27/04/2019
 * Time: 18:25
 */
@Singleton
class FilterServiceImpl @Inject constructor(
    @UserFilters private val userFilterRepo: FilterRepository,
    @SystemFilters private val systemFilterRepo: FilterRepository,
    private val gameProviderService: GameProviderService,
    eventBus: EventBus
) : FilterService {
    private val log = logger()

    override val userFilters = userFilterRepo.filters

    init {
        userFilters.broadcastTo(eventBus, NamedFilter::id, FilterEvent::Added, FilterEvent::Deleted, FilterEvent::Updated)

        if (userFilters.isEmpty()) {
            log.info("Creating default user filters...")
            defaultUserFilters.forEach { userFilterRepo.add(it) }
        }
    }

    override fun get(id: FilterId) = userFilters.find { it.id == id } ?: throw IllegalArgumentException("Filter($id) doesn't exist!")

    override fun add(data: NamedFilterData) = task("Adding Filter '${data.name}'...") {
        successMessage = { "Added Filter: '${data.name}'." }
        userFilterRepo.add(data)
    }

    override fun update(filter: NamedFilter, data: NamedFilterData) = task("Updating Filter '${filter.name}'...") {
        val updatedFilter = userFilterRepo.update(filter, data)
        successMessage = { "Updated Filter: '${updatedFilter.name}'." }
        updatedFilter
    }

    override fun delete(filter: NamedFilter) = task("Deleting Filter '${filter.name}'...") {
        successMessage = { "Deleted Filter: '${filter.name}'." }
        userFilterRepo.delete(filter)
    }

    override fun getSystemFilter(name: String): Filter? = systemFilter(name)?.filter

    override fun putSystemFilter(name: String, filter: Filter) {
        val systemFilter = systemFilter(name)
        if (systemFilter != null) {
            systemFilterRepo.update(systemFilter, systemFilter.data.copy(filter = filter))
        } else {
            systemFilterRepo.add(NamedFilterData(name, filter, isTag = false))
        }
    }

    override fun filter(games: List<Game>, filter: Filter): List<Game> {
        return if (filter.isEmpty) {
            games
        } else {
            val context = createContext()
            log.time("Filtering ${games.size} games...", { timeTaken, results -> "${results.size} results in $timeTaken" }, Logger::trace) {
                games.filter { filter.evaluate(it, context) }
            }
        }
    }

//    private fun calc(report: Report, games: List<Game>) = task("Calculating report '${report.name}'...") {
//        val context = filterService.createContext()
//
//        totalItems = games.size
//        // Report progress every 'chunkSize' games.
//        val chunkSize = 50
//        val matchingGames = games.chunked(chunkSize).flatMapIndexed { i, chunk ->
//            val result = chunk.filter { game ->
//                !report.excludedGames.contains(game.id) && report.filter.evaluate(game, context)
//            }
//            processedItems = i * chunkSize + chunk.size
//            result
//        }
//        ReportResult(matchingGames.sortedBy { it.name })
//    }

    override fun calcFilterTags(game: Game): List<TagId> {
        val context = createContext()
        return userFilters.mapNotNull { filter ->
            if (filter.isTag && filter.filter.evaluate(game, context)) {
                filter.name
            } else {
                null
            }
        }
    }

    fun invalidate() = userFilterRepo.invalidate()

    private fun systemFilter(name: String): NamedFilter? = systemFilterRepo.filters.find { it.name == name }

    private fun createContext() = object : Filter.Context {
        override val now = com.gitlab.ykrasik.gamedex.util.now

        override fun providerSupports(providerId: ProviderId, platform: Platform) =
            gameProviderService.allProviders.find { it.id == providerId }?.supports(platform) == true
    }

    private companion object {
        val defaultUserFilters = listOf(
            NamedFilterData("Low Score", Filter.CriticScore(60.0).not or Filter.UserScore(60.0).not, isTag = true),
            NamedFilterData("No Score", Filter.CriticScore(0.0).not and Filter.UserScore(0.0).not, isTag = true),
            NamedFilterData("Not Updated Recently", Filter.PeriodUpdateDate(2.months).not, isTag = true)
        )
    }
}

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class UserFilters

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class SystemFilters