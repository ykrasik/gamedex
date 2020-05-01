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

package com.gitlab.ykrasik.gamedex.app.api.game

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.app.api.util.SettableList
import com.gitlab.ykrasik.gamedex.app.api.util.UserMutableState

/**
 * User: ykrasik
 * Date: 06/06/2018
 * Time: 09:43
 */
interface ViewWithPlatform {
    val availablePlatforms: SettableList<AvailablePlatform>

    val currentPlatform: UserMutableState<AvailablePlatform>
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = AvailablePlatform.SinglePlatform::class, name = "platform"),
    JsonSubTypes.Type(value = AvailablePlatform.All::class, name = "all")
)
sealed class AvailablePlatform {
    data class SinglePlatform(val platform: Platform) : AvailablePlatform() {
        override fun toString() = platform.toString()
    }

    object All : AvailablePlatform() {
        override fun toString() = "All"
    }

    fun matches(platform: Platform) = when (this) {
        is All -> true
        is SinglePlatform -> this.platform == platform
    }

    companion object {
        val values = listOf(All) + Platform.values().map(::SinglePlatform)
    }
}