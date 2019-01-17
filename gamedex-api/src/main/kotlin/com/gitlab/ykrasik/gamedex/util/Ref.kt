/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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

import kotlinx.atomicfu.atomic

/**
 * User: ykrasik
 * Date: 16/01/2019
 * Time: 20:05
 */
interface Ref<T> {
    val value: T
}

class MutableAtomicRef<T>(initial: T) : Ref<T> {
    private val ref = atomic(initial)

    override var value
        get() = ref.value
        set(value) {
            ref.value = value
        }

    override fun toString() = value.toString()
}

fun <T> ref(initial: T) = MutableAtomicRef(initial)