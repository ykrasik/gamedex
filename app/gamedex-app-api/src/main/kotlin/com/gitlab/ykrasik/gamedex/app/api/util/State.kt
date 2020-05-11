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

package com.gitlab.ykrasik.gamedex.app.api.util

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * User: ykrasik
 * Date: 02/12/2018
 * Time: 16:44
 *
 * When a view exposes a [MutableStateFlow], by convention the view should not write to that flow,
 * that flow is for presenters to update the view's state.
 * When a view exposes a [ViewMutableStateFlow],  by convention the view has permissions to write to this flow as well,
 * to notify about changes to its' data.
 * Presenters should react to these changes.
 */
interface ViewMutableStateFlow<T> : MutableStateFlow<Value<T>> {
    val v: T get() = value.value

    var valueFromView: T
        get() = checkNotNull(value as? Value.FromView) { "Value is not from view!" }.value
        set(value) {
            this.value = value.fromView
        }

    var valueFromPresenter: T
        get() = checkNotNull(value as? Value.FromPresenter) { "Value is not from presenter!" }.value
        set(value) {
            this.value = value.fromPresenter
        }
}

sealed class Value<T> {
    abstract val value: T

    data class FromPresenter<T>(override val value: T) : Value<T>() {
        override fun asFromView() = value.fromView
    }

    data class FromView<T>(override val value: T) : Value<T>() {
        override fun asFromView() = this
    }

    abstract fun asFromView(): FromView<T>
}

val <T> T.fromView: Value.FromView<T> get() = Value.FromView(this)
val <T> T.fromPresenter: Value.FromPresenter<T> get() = Value.FromPresenter(this)