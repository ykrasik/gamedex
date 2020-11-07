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

package com.gitlab.ykrasik.gamedex.app.api.settings

import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.util.IsValid
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * User: ykrasik
 * Date: 20/06/2018
 * Time: 09:25
 */
interface ProviderOrderSettingsView {
    val canChangeProviderOrder: MutableStateFlow<IsValid>

    val search: ViewMutableStateFlow<Order>
    val name: ViewMutableStateFlow<Order>
    val description: ViewMutableStateFlow<Order>
    val releaseDate: ViewMutableStateFlow<Order>
    val thumbnail: ViewMutableStateFlow<Order>
    val poster: ViewMutableStateFlow<Order>
    val screenshot: ViewMutableStateFlow<Order>
}

typealias Order = List<ProviderId>

val minOrder = -1