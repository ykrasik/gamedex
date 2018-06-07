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

import com.gitlab.ykrasik.gamedex.app.api.ViewRegistry
import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.ButtonBase
import javafx.scene.control.TextInputControl
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.javafx.JavaFx
import org.controlsfx.glyphfont.Glyph
import tornadofx.*

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView(title: String? = null, icon: Glyph? = null) : View(title, icon) {
    private val taskRunner: TaskRunner by di()
    protected val viewRegistry: ViewRegistry by di()

    val enabledProperty = SimpleBooleanProperty(false)

    // All tabs (which we use as screens) will have 'onDock' called even though they're not actually showing.
    // This is just how TornadoFx works.
    protected var skipFirstDock = false

    init {
        taskRunner.currentlyRunningTaskChannel.subscribe(JavaFx) {
            enabledProperty.value = it == null
        }

        whenDocked {
            if (!skipFirstDock) {
                viewRegistry.onShow(this)
            }
            skipFirstDock = false
        }
        whenUndocked {
            if (!skipFirstDock) {
                viewRegistry.onHide(this)
            }
        }
    }

    fun <T, O : ObservableValue<T>> O.eventOnChange(channel: Channel<T>) = eventOnChange(channel) { it }

    inline fun <T, R, O : ObservableValue<out T>> O.eventOnChange(channel: Channel<R>, crossinline factory: (T) -> R) = apply {
        onChange { channel.event(factory(it!!)) }
    }

    // TODO: Find a better name
    fun ButtonBase.eventOnAction(channel: Channel<Unit>) = apply {
        setOnAction { channel.event(Unit) }
    }

    // TODO: Find a better name
    inline fun <T> ButtonBase.eventOnAction(channel: Channel<T>, crossinline f: () -> T) = apply {
        setOnAction { channel.event(f()) }
    }

    fun ViewModel.presentableStringProperty(channel: Channel<String>): Property<String> =
        presentableProperty(channel) { SimpleStringProperty("") }

    inline fun <R> ViewModel.presentableStringProperty(channel: Channel<R>,
                                                       crossinline factory: (String) -> R): Property<String> =
        presentableProperty(channel, { SimpleStringProperty("") }, factory)

    inline fun <reified T : Any, reified O : Property<T>> ViewModel.presentableProperty(channel: Channel<T>,
                                                                                        crossinline propertyFactory: () -> O): O =
        presentableProperty(channel, propertyFactory) { it }

    inline fun <reified T : Any, R, reified O : Property<T>> ViewModel.presentableProperty(channel: Channel<R>,
                                                                                           crossinline propertyFactory: () -> O,
                                                                                           crossinline valueFactory: (T) -> R): O =
        bind<O, T, O> { propertyFactory() }.apply {
            onChange {
                channel.event(valueFactory(it!!))
                commit()
            }
        }

    fun TextInputControl.validatorFrom(viewModel: ViewModel, errorValue: ObservableValue<String?>) {
        errorValue.onChange { viewModel.validate() }
        validator(ValidationTrigger.None) {
            errorValue.value?.let { error(it) }
        }
    }

    fun <E> Channel<E>.event(e: E) = offer(e)
}