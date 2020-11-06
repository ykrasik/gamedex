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

package com.gitlab.ykrasik.gamedex.core.provider

import com.gitlab.ykrasik.gamedex.core.storage.StringIdJsonStorageFactory
import com.gitlab.ykrasik.gamedex.core.storage.memoryCached
import com.gitlab.ykrasik.gamedex.provider.ProviderId
import com.gitlab.ykrasik.gamedex.provider.ProviderStorage
import com.gitlab.ykrasik.gamedex.provider.ProviderStorageFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 31/10/2020
 * Time: 10:38
 */
@Singleton
class ProviderStorageFactoryImpl @Inject constructor() : ProviderStorageFactory {
    override fun <V : Any> create(id: ProviderId, klass: KClass<V>) = object : ProviderStorage<V> {
        private val storage = StringIdJsonStorageFactory("data/provider/$id", klass).memoryCached()

        override fun get() = storage.get(key)
        override fun set(v: V) = storage.set(key, v)
    }
}

private const val key = "data"