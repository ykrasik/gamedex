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

package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.app.api.Presenter
import com.gitlab.ykrasik.gamedex.app.api.View
import com.gitlab.ykrasik.gamedex.app.api.util.launchConsumeEach
import com.gitlab.ykrasik.gamedex.core.api.util.uiThreadDispatcher
import com.gitlab.ykrasik.gamedex.util.InitOnce

/**
 * User: ykrasik
 * Date: 24/04/2018
 * Time: 08:17
 */
abstract class BasePresenter<E, V : View<E>> : Presenter<V> {
    protected var view: V by InitOnce()

    override fun present(view: V) {
        this.view = view
        initView(view)
        view.events.launchConsumeEach(uiThreadDispatcher) {
            try {
                handleEvent(it)
            } catch (e: Exception) {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
            }
        }
    }

    protected abstract fun initView(view: V)
    protected abstract suspend fun handleEvent(event: E)
}