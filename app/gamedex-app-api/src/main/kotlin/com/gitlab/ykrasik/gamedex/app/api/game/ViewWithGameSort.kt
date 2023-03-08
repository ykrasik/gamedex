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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.gitlab.ykrasik.gamedex.app.api.util.ViewMutableStateFlow

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 17:47
 */
interface ViewWithGameSort {
    val sortBy: ViewMutableStateFlow<SortBy>

    val sortOrder: ViewMutableStateFlow<SortOrder>
}

enum class SortBy(val displayName: String) {
    Name("Name"),
    CriticScore("Critic Score"),
    UserScore("User Score"),
    AvgScore("Average Score"),
    MinScore("Min Score"),
    MaxScore("Max Score"),
    Size("Size"),
    ReleaseDate("Release Date"),
    CreateDate("Create Date"),
    UpdateDate("Update Date")
}

enum class SortOrder(val displayName: String) {
    Asc("Ascending"),
    Desc("Descending");

    fun toggle(): SortOrder = when (this) {
        Asc -> Desc
        Desc -> Asc
    }
}