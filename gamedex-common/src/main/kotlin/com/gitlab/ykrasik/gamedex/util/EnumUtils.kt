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

package com.gitlab.ykrasik.gamedex.util

import java.util.*

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 11:55
 */
interface IdentifiableEnum<out K> {
    val key: K
}

class EnumIdConverter<K, V>(valueType: Class<V>) where V : Enum<V>, V : IdentifiableEnum<K> {
    private val map = HashMap<K, V>()

    init {
        for (v in valueType.enumConstants) {
            map.put(v.key, v)
        }
    }

    operator fun get(key: K): V = map[key]!!
}