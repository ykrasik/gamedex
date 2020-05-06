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
import com.gitlab.ykrasik.gamedex.core.Presenter
import com.gitlab.ykrasik.gamedex.core.ViewSession
import com.gitlab.ykrasik.gamedex.core.filter.FilterService
import com.gitlab.ykrasik.gamedex.core.task.TaskService
import com.gitlab.ykrasik.gamedex.util.Try
import com.gitlab.ykrasik.gamedex.util.and
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
            view.initialNamedFilter.forEach { initialNamedFilter ->
                view.name *= initialNamedFilter.id
                view.filter *= initialNamedFilter.filter
                view.isTag *= if (!initialNamedFilter.isAnonymous) initialNamedFilter.isTag else true
            }
            view.name.mapTo(view.nameIsValid) { name ->
                Try { check(name.isNotBlank()) { "Name is required!" } }
            }

            view.name.combineLatest(view.nameIsValid, view.filter, view.filterIsValid, view.isTag) { name, nameIsValid, filter, filterIsValid, isTag ->
                view.canAccept *= nameIsValid and filterIsValid and Try {
                    val initialNamedFilter = view.initialNamedFilter.value
                    check(!filter.isEmpty) { "Filter is empty!" }
                    if (name == initialNamedFilter.id &&
                        filter.isEqual(initialNamedFilter.filter) &&
                        isTag == initialNamedFilter.isTag/* &&
                            view.excludedGames.map { it.id } == view.report?.excludedGames*/) {
                        error("Nothing changed!")
                    }
                    if (isTag && filter.find(Filter.FilterTag::class) != null) {
                        error("Filters that tag games may not depend on FilterTag filters!")
                    }
                }
            }

//            view.unexcludeGameActions.forEach { onUnexcludeGame(it) }
            view.acceptActions.forEach { onAccept() }
            view.cancelActions.forEach { onCancel() }
        }

//        private fun onUnexcludeGame(game: Game) {
//            view.excludedGames -= game
//            setCanAccept()
//        }

        private suspend fun onAccept() {
            view.canAccept.assert()

            val name = view.name.value
            val initialNamedFilter = view.initialNamedFilter.value

            val isRenamed = name != initialNamedFilter.id
            val filterToOverwrite = if (isRenamed) {
                filterService.userFilters.find { it.id == name }
            } else {
                null
            }
            if (filterToOverwrite != null && !view.confirmOverwrite(filterToOverwrite)) {
                // There already is a filter with such a name which is not the filter we are editing, confirm user wants to overwrite.
                return
            }

            val namedFilter = NamedFilter(
                id = name,
                filter = view.filter.value,
                isTag = view.isTag.value
            )

            taskService.execute(filterService.save(namedFilter))
            if (!initialNamedFilter.isAnonymous && isRenamed) {
                taskService.execute(filterService.delete(initialNamedFilter))
            }

            hideView()
        }

        private fun onCancel() {
            hideView()
        }

        private fun hideView() = eventBus.requestHideView(view)
    }
}