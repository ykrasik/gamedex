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

package com.gitlab.ykrasik.gamedex.provider

import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 31/10/2020
 * Time: 10:14
 */
interface ProviderStorage<V> {
    fun get(): V?
    fun set(v: V)
    fun reset()
}

interface ProviderStorageFactory {
    fun <V : Any> create(id: ProviderId, klass: KClass<V>): ProviderStorage<V>
}

inline fun <reified V : Any> ProviderStorageFactory.create(id: ProviderId): ProviderStorage<V> =
    create(id, V::class)