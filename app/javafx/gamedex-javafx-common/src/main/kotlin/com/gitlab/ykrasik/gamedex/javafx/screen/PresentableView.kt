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

import com.gitlab.ykrasik.gamedex.app.api.Presenter
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.scene.control.ButtonBase
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 21/04/2018
 * Time: 07:13
 */
abstract class PresentableView<out P : Presenter<*>>(presenterClass: KClass<P>, title: String? = null, icon: Glyph? = null)
    : View(title, icon), com.gitlab.ykrasik.gamedex.app.api.View {
    // There's runtime IllegalAccessErrors thrown if this is made protected :(
    val presenter: P = FX.dicontainer!!.getInstance(presenterClass)

    protected val enabledProperty = SimpleBooleanProperty(false)
    override var enabled by enabledProperty

    // There's runtime IllegalAccessErrors thrown if this is made protected :(
    fun <T, O : ObservableValue<T>> O.presentOnChange(call: (P, T) -> Unit) = apply {
        onChange { call(presenter, it!!) }
    }

    // There's runtime IllegalAccessErrors thrown if this is made protected :(
    inline fun <reified T : Any, reified O : Property<T>> ViewModel.presentableProperty(crossinline call: (P, T) -> Unit,
                                                                                        crossinline propertyFactory: () -> O): Property<T> =
        bind<O, T, O> { propertyFactory() }.apply {
            onChange {
                call(presenter, it!!)
                commit()
            }
        }

    protected inline fun ButtonBase.presentOnAction(crossinline call: (P) -> Unit) {
        setOnAction {
            call(presenter)
        }
    }
}