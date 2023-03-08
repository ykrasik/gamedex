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

package com.gitlab.ykrasik.gamedex.core.module

import com.gitlab.ykrasik.gamedex.core.view.Presenter
import com.gitlab.ykrasik.gamedex.util.logger
import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.MapBinder
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 19/09/2018
 * Time: 00:36
 */
abstract class InternalCoreModule : AbstractModule() {
    protected val log = logger("Core")

    protected inline fun <T : Presenter<V>, reified V> bindPresenter(klass: KClass<T>) {
        MapBinder.newMapBinder(binder(), object : TypeLiteral<KClass<*>>() {}, object : TypeLiteral<Presenter<*>>() {})
            .addBinding(V::class).to(klass.java)
    }
}