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

package com.gitlab.ykrasik.gamedex.core.api.provider

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ProviderData
import com.gitlab.ykrasik.gamedex.ProviderHeader
import com.gitlab.ykrasik.gamedex.core.api.task.Task
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import java.io.File

/**
 * User: ykrasik
 * Date: 04/04/2018
 * Time: 21:58
 */
// TODO: Make this a presenter!!!
interface GameProviderService {
    suspend fun search(taskData: ProviderTaskData, excludedProviders: List<ProviderId>): SearchResults?

    suspend fun download(taskData: ProviderTaskData, headers: List<ProviderHeader>): List<ProviderData>
}

data class ProviderTaskData(
    val task: Task,
    val name: String,
    val platform: Platform,
    val path: File
)

data class SearchResults(
    val providerData: List<ProviderData>,
    val excludedProviders: List<ProviderId>
) {
    fun isEmpty(): Boolean = providerData.isEmpty() && excludedProviders.isEmpty()
}