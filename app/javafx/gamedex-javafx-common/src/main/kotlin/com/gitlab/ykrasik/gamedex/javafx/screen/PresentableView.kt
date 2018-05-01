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

import com.gitlab.ykrasik.gamedex.app.api.task.TaskRunner
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.ButtonBase
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import org.controlsfx.glyphfont.Glyph
import tornadofx.View
import tornadofx.ViewModel
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView(title: String? = null, icon: Glyph? = null) : View(title, icon) {
    private val taskRunner: TaskRunner by di()

    val enabledProperty = SimpleBooleanProperty(false)

    init {
        taskRunner.currentlyRunningTaskChannel.subscribe(JavaFx) {
            enabledProperty.value = it == null
        }
    }

    inline fun <T, O : ObservableValue<T>> O.presentOnChange(crossinline call: suspend (T) -> Unit) = apply {
        onChange { present { call(it!!) } }
    }

    inline fun <reified T : Any, reified O : Property<T>> ViewModel.presentableProperty(crossinline call: suspend (T) -> Unit,
                                                                                        crossinline propertyFactory: () -> O): Property<T> =
        bind<O, T, O> { propertyFactory() }.apply {
            onChange {
                present { call(it!!) }
                commit()
            }
        }

    fun ButtonBase.presentOnAction(f: suspend () -> Unit) {
        setOnAction { present(f) }
    }

    fun present(f: suspend () -> Unit) {
        launch(JavaFx) {
            f()
        }
    }
}