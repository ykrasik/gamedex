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

package com.gitlab.ykrasik.gamedex.app.api.filter

import com.gitlab.ykrasik.gamedex.Genre
import com.gitlab.ykrasik.gamedex.Library
import com.gitlab.ykrasik.gamedex.TagId
import com.gitlab.ykrasik.gamedex.app.api.util.MultiReadChannel
import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import com.gitlab.ykrasik.gamedex.app.api.util.StatefulChannel
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStatefulChannel
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 02/06/2018
 * Time: 10:20
 */
interface FilterView {
    val filter: ViewMutableStatefulChannel<Filter>
    val filterIsValid: StatefulChannel<IsValid>

    val availableFilters: SettableList<KClass<out Filter.Rule>>

    val availableLibraries: SettableList<Library>
    val availableGenres: SettableList<Genre>
    val availableTags: SettableList<TagId>
    val availableFilterTags: SettableList<TagId>
    val availableProviderIds: SettableList<ProviderId>

    val wrapInAndActions: MultiReadChannel<Filter>
    val wrapInOrActions: MultiReadChannel<Filter>
    val wrapInNotActions: MultiReadChannel<Filter>
    val unwrapNotActions: MultiReadChannel<Filter.Not>
    val updateFilterActions: MultiReadChannel<Pair<Filter.Rule, Filter.Rule>>
    val replaceFilterActions: MultiReadChannel<Pair<Filter, KClass<out Filter>>>
    val deleteFilterActions: MultiReadChannel<Filter>
}