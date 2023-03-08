/****************************************************************************
 * Copyright (C) 2016-2023 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.app.api.filter

import com.gitlab.ykrasik.gamedex.Genre
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.TagId
import com.gitlab.ykrasik.gamedex.app.api.util.ValidatedValue
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 10:20
 */
interface FilterView {
    val filter: ViewMutableStateFlow<Filter>
    val filterValidatedValue: MutableStateFlow<ValidatedValue<Filter>>

    val availableFilters: MutableStateFlow<List<KClass<out Filter.Rule>>>

    val availableLibraries: MutableStateFlow<List<Library>>
    val availableGenres: MutableStateFlow<List<Genre>>
    val availableTags: MutableStateFlow<List<TagId>>
    val availableFilterTags: MutableStateFlow<List<TagId>>
    val availableProviderIds: MutableStateFlow<List<ProviderId>>

    val wrapInAndActions: Flow<Filter>
    val wrapInOrActions: Flow<Filter>
    val wrapInNotActions: Flow<Filter>
    val unwrapNotActions: Flow<Filter.Not>
    val updateFilterActions: Flow<Pair<Filter.Rule, Filter.Rule>>
    val replaceFilterActions: Flow<Pair<Filter, KClass<out Filter>>>
    val deleteFilterActions: Flow<Filter>
}