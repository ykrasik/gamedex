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

package com.gitlab.ykrasik.gamedex.core.api

import com.gitlab.ykrasik.gamedex.core.api.general.GeneralSettingsPresenter
import com.gitlab.ykrasik.gamedex.core.api.library.EditLibraryPresenter
import com.gitlab.ykrasik.gamedex.core.api.library.LibraryPresenter
import com.gitlab.ykrasik.gamedex.util.InitOnceGlobal
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 16:55
 */
interface Presenter<V : View<*>> {
    fun present(view: V)
}

interface View<Event> {
    val events: ReceiveChannel<Event>
}

// This value is set after pre-loading is complete.
var presenters: Presenters by InitOnceGlobal()

@Singleton
class Presenters @Inject constructor(
    val libraryPresenter: LibraryPresenter,
    val editLibraryPresenter: EditLibraryPresenter,
    val generalSettingsPresenter: GeneralSettingsPresenter
)