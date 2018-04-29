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

import com.gitlab.ykrasik.gamedex.app.api.ViewCanRunTask
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.experimental.channels.Channel
import org.controlsfx.glyphfont.Glyph
import tornadofx.*

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView<E>(title: String? = null, icon: Glyph? = null) :
    View(title, icon), com.gitlab.ykrasik.gamedex.app.api.View<E> {

    override val events = Channel<E>(capacity = 32)

    protected fun sendEvent(event: E) = events.offer(event)

    protected fun <T, P : ObservableValue<T>> P.eventOnChange(factory: (T) -> E) = apply {
        onChange { sendEvent(factory(it!!)) }
    }

    // There's runtime IllegalAccessErrors thrown if this is made protected :(
    inline fun <reified T : Any, reified P : Property<T>> ViewModel.presentableProperty(crossinline eventFactory: (T) -> E,
                                                                                        crossinline propertyFactory: () -> P): Property<T> =
        bind<P, T, P> { propertyFactory() }.apply {
            onChange {
                events.offer(eventFactory(it!!))
                commit()
            }
        }
}

abstract class PresentableViewCanRunTask<E>(title: String? = null, icon: Glyph? = null) : PresentableView<E>(title, icon), ViewCanRunTask<E> {
    protected val canRunTaskProperty = SimpleBooleanProperty(false)
    override var canRunTask by canRunTaskProperty
}