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

package com.gitlab.ykrasik.gamedex.app.api.game

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 17:47
 */
interface ViewCanChangeGameSort {
    var sortBy: SortBy
    val sortByChanges: ReceiveChannel<SortBy>

    var sortOrder: SortOrder
    val sortOrderChanges: ReceiveChannel<SortOrder>
}

enum class SortBy(val displayName: String) {
    name_("Name"),
    criticScore("Critic Score"),
    userScore("User Score"),
    avgScore("Average Score"),
    minScore("Min Score"),
    maxScore("Max Score"),
    size("Size"),
    releaseDate("Release Date"),
    createDate("Create Date"),
    updateDate("Update Date");

    override fun toString() = displayName
}

enum class SortOrder(val displayName: String) {
    asc("Ascending"),
    desc("Descending");

    fun toggle(): SortOrder = when (this) {
        asc -> desc
        desc -> asc
    }

    override fun toString() = displayName
}