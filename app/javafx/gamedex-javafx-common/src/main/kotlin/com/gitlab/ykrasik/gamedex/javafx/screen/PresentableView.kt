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

import com.gitlab.ykrasik.gamedex.core.api.ViewModel
import com.gitlab.ykrasik.gamedex.javafx.skipFirstTime
import org.controlsfx.glyphfont.Glyph
import tornadofx.View

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView<Event, Action, VM>(
    title: String,
    icon: Glyph?,
    private val presenter: () -> VM,
    private val skipFirst: Boolean = false
) : View(title, icon) where VM : ViewModel<Event, Action> {
    private var viewModel: VM? = null

    protected suspend fun sendEvent(event: Event) = viewModel!!.events.send(event)

    // This is first called when the mainView is loaded (even though this view isn't shown) - skip first time.
    override fun onDock() {
        if (skipFirst) {
            skipFirstTime(this::present)
        } else {
            present()
        }
    }

    private fun present() {
        require(viewModel == null) { "$this: Already presenting!" }
        viewModel = presenter().apply {
            onPresent()
            consumeActions { action ->
                onAction(action)
            }
        }
    }

    override fun onUndock() {
        viewModel!!.close()
        viewModel = null
    }

    protected abstract fun VM.onPresent()

    protected abstract suspend fun onAction(action: Action)
}