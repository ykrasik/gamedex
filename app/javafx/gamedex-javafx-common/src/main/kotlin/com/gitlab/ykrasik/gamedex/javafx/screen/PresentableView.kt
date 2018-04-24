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

package com.gitlab.ykrasik.gamedex.javafx.screen

import com.gitlab.ykrasik.gamedex.core.api.Presenter
import kotlinx.coroutines.experimental.channels.Channel
import org.controlsfx.glyphfont.Glyph
import tornadofx.View

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView<E>(title: String, icon: Glyph?, presenter: Presenter<com.gitlab.ykrasik.gamedex.core.api.View<E>>) :
    View(title, icon), com.gitlab.ykrasik.gamedex.core.api.View<E> {

    override val events = Channel<E>(capacity = 32)

    init {
        presenter.present(this)
    }

    protected fun sendEvent(event: E) = events.offer(event)
}