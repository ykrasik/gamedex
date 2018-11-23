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

package com.gitlab.ykrasik.gamedex.app.javafx.settings

import com.gitlab.ykrasik.gamedex.app.api.settings.ImageDisplayType
import com.gitlab.ykrasik.gamedex.app.api.settings.MutableGameWallDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.eventOnChange
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 12:08
 */
class JavaFxGameWallDisplaySettings : MutableGameWallDisplaySettings {
    override val imageDisplayTypeChanges = channel<ImageDisplayType>()
    val imageDisplayTypeProperty = SimpleObjectProperty<ImageDisplayType>().eventOnChange(imageDisplayTypeChanges)
    override var imageDisplayType by imageDisplayTypeProperty

    override val showBorderChanges = channel<Boolean>()
    val showBorderProperty = SimpleBooleanProperty().eventOnChange(showBorderChanges)
    override var showBorder by showBorderProperty

    override val widthChanges = channel<Double>()
    val widthProperty = SimpleDoubleProperty().eventOnChange(widthChanges) { it.toDouble() }
    override var width by widthProperty

    override val heightChanges = channel<Double>()
    val heightProperty = SimpleDoubleProperty().eventOnChange(heightChanges) { it.toDouble() }
    override var height by heightProperty

    override val horizontalSpacingChanges = channel<Double>()
    val horizontalSpacingProperty = SimpleDoubleProperty().eventOnChange(horizontalSpacingChanges) { it.toDouble() }
    override var horizontalSpacing by horizontalSpacingProperty

    override val verticalSpacingChanges = channel<Double>()
    val verticalSpacingProperty = SimpleDoubleProperty().eventOnChange(verticalSpacingChanges) { it.toDouble() }
    override var verticalSpacing by verticalSpacingProperty

    inline fun onChange(crossinline f: () -> Unit) = listOf(
        imageDisplayTypeProperty, showBorderProperty, widthProperty, heightProperty, horizontalSpacingProperty, verticalSpacingProperty
    ).forEach { it.onChange { f() } }
}