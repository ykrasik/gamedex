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

package com.gitlab.ykrasik.gamedex.app.api.provider

import com.gitlab.ykrasik.gamedex.app.api.ConfirmationView
import com.gitlab.ykrasik.gamedex.app.api.filter.Filter
import com.gitlab.ykrasik.gamedex.app.api.util.ValidatedValue
import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow

/**
 * User: ykrasik
 * Date: 31/12/2018
 * Time: 15:49
 */
interface SyncGamesWithMissingProvidersView : ConfirmationView {
    val bulkSyncGamesFilter: ViewMutableStateFlow<Filter>
    val bulkSyncGamesFilterValidatedValue: ViewMutableStateFlow<ValidatedValue<Filter>>

    val syncOnlyMissingProviders: ViewMutableStateFlow<Boolean>
}