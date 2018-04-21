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

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 16:45
 */
class InitOnceGlobal<T> : ReadWriteProperty<Nothing?, T> {
    private var value: Any? = EMPTY

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Nothing?, property: KProperty<*>): T {
        check(value != EMPTY) { "Value wasn't initialized yet!" }
        return value as T
    }

    override fun setValue(thisRef: Nothing?, property: KProperty<*>, value: T) {
        check(this.value == EMPTY) { "Value was already initialized: ${this.value}" }
        this.value = value
    }
}

class InitOnce<T> : ReadWriteProperty<Any, T> {
    private var value: Any? = EMPTY

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        check(value != EMPTY) { "Value wasn't initialized yet!" }
        return value as T
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        check(this.value == EMPTY) { "Value was already initialized: ${this.value}" }
        this.value = value
    }
}

private object EMPTY