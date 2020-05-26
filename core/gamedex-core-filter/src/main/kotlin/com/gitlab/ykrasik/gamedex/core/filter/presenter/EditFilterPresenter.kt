/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.filter.presenter

import com.gitlab.ykrasik.gamedex.app.api.filter.*
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.core.view.ViewSession
import com.gitlab.ykrasik.gamedex.util.IsValid
import com.gitlab.ykrasik.gamedex.util.and
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 24/06/2018
 * Time: 10:33
 */
@Singleton
class EditFilterPresenter @Inject constructor(
    private val filterService: FilterService,
    private val taskService: TaskService,
    private val eventBus: EventBus
) : Presenter<EditFilterView> {
    override fun present(view: EditFilterView) = object : ViewSession() {
        init {
            view.name *= view.initialNamedFilter.onlyChangesFromView().map { it.id } withDebugName "name"
            view.filter *= view.initialNamedFilter.onlyChangesFromView().map { it.filter } withDebugName "filter"
            view.isTag *= view.initialNamedFilter.onlyChangesFromView().map { if (!it.isAnonymous) it.isTag else true } withDebugName "isTag"

            view::nameIsValid *= view.name.allValues().map { name ->
                IsValid { check(name.isNotBlank()) { "Name is required!" } }
            }

            view::canAccept *= combine(
                view.nameIsValid,
                view.filterValidatedValue.allValues(),
                view.isTag.allValues()
            ) { nameIsValid, filterValidatedValue, isTag ->
                nameIsValid and filterValidatedValue.isValid and IsValid {
                    val filter = filterValidatedValue.value
                    check(!filter.isEmpty) { "Filter is empty!" }
                    if (isTag && filter.find(Filter.FilterTag::class) != null) {
                        error("Filters that tag games may not depend on FilterTag filters!")
                    }
                }
            }

//            view::unexcludeGameActions.forEach { onUnexcludeGame(it) }
            view::acceptActions.forEach { onAccept() }
            view::cancelActions.forEach { onCancel() }
        }

//        private fun onUnexcludeGame(game: Game) {
//            view.excludedGames -= game
//            setCanAccept()
//        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            val initialNamedFilter = view.initialNamedFilter.v
            val newNamedFilter = NamedFilter(
                id = view.name.v,
                filter = view.filter.v,
                isTag = view.isTag.v
            )
            if (newNamedFilter != initialNamedFilter.copy(timestamp = newNamedFilter.timestamp)) {
                val isRenamed = newNamedFilter.id != initialNamedFilter.id
                val filterToOverwrite = if (isRenamed) {
                    filterService.userFilters.find { it.id == newNamedFilter.id }
                } else {
                    null
                }
                if (filterToOverwrite != null && !view.confirmOverwrite(filterToOverwrite)) {
                    // There already is a filter with such a name which is not the filter we are editing, confirm user wants to overwrite.
                    return
                }

                taskService.execute(filterService.save(newNamedFilter))
                if (!initialNamedFilter.isAnonymous && isRenamed) {
                    taskService.execute(filterService.delete(initialNamedFilter))
                }
            }

            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}