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

package com.gitlab.ykrasik.gamedex.core.view

import com.gitlab.ykrasik.gamedex.app.api.ViewManager

/**
 * User: ykrasik
 * Date: 26/05/2020
 * Time: 21:31
 */
interface ViewService {
    suspend fun <V : Any> showAndHide(show: ViewManager.() -> V, hide: ViewManager.(V) -> Unit)
    suspend fun <V : Any, Params> showAndHide(show: ViewManager.(Params) -> V, hide: ViewManager.(V) -> Unit, params: Params)
}