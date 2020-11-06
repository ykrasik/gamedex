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

package com.gitlab.ykrasik.gamedex.test

import com.gitlab.ykrasik.gamedex.provider.ProviderStorage

/**
 * User: ykrasik
 * Date: 31/10/2020
 * Time: 12:58
 */
class MockProviderStorage<V>(var value: V? = null) : ProviderStorage<V> {
    override fun get() = value
    override fun set(v: V) {
        value = v
    }

    override fun reset() {
        this.value = null
    }
}