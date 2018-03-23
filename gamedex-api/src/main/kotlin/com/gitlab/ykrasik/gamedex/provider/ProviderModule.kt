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

package com.gitlab.ykrasik.gamedex.provider

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

/**
 * User: ykrasik
 * Date: 26/03/2018
 * Time: 09:57
 */
abstract class ProviderModule : AbstractModule() {
    protected inline fun <reified T : GameProvider> bindProvider() {
        Multibinder.newSetBinder(binder(), GameProvider::class.java).addBinding().to(T::class.java)
    }
}