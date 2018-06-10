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

import com.gitlab.ykrasik.gamedex.app.api.settings.DisplayPosition
import com.gitlab.ykrasik.gamedex.app.api.settings.MutableOverlayDisplaySettings
import com.gitlab.ykrasik.gamedex.app.api.util.channel
import com.gitlab.ykrasik.gamedex.javafx.eventOnChange
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 10/06/2018
 * Time: 14:29
 */
class JavaFxOverlayDisplaySettings : MutableOverlayDisplaySettings {
    override val enabledChanges = channel<Boolean>()
    val enabledProperty = SimpleBooleanProperty().eventOnChange(enabledChanges)
    override var enabled by enabledProperty

    override val showOnlyWhenActiveChanges = channel<Boolean>()
    val showOnlyWhenActiveProperty = SimpleBooleanProperty().eventOnChange(showOnlyWhenActiveChanges)
    override var showOnlyWhenActive by showOnlyWhenActiveProperty

    override val positionChanges = channel<DisplayPosition>()
    val positionProperty = SimpleObjectProperty<DisplayPosition>().eventOnChange(positionChanges)
    override var position by positionProperty

    override val fillWidthChanges = channel<Boolean>()
    val fillWidthProperty = SimpleBooleanProperty().eventOnChange(fillWidthChanges)
    override var fillWidth by fillWidthProperty

    override val fontSizeChanges = channel<Int>()
    val fontSizeProperty = SimpleIntegerProperty().eventOnChange(fontSizeChanges) { it.toInt() }
    override var fontSize by fontSizeProperty

    override val boldFontChanges = channel<Boolean>()
    val boldFontProperty = SimpleBooleanProperty().eventOnChange(boldFontChanges)
    override var boldFont by boldFontProperty

    override val italicFontChanges = channel<Boolean>()
    val italicFontProperty = SimpleBooleanProperty().eventOnChange(italicFontChanges)
    override var italicFont by italicFontProperty

    override val textColorChanges = channel<String>()
    val textColorProperty = SimpleObjectProperty<String>().eventOnChange(textColorChanges)
    override var textColor by textColorProperty

    override val backgroundColorChanges = channel<String>()
    val backgroundColorProperty = SimpleObjectProperty<String>().eventOnChange(backgroundColorChanges)
    override var backgroundColor by backgroundColorProperty

    override val opacityChanges = channel<Double>()
    val opacityProperty = SimpleDoubleProperty().eventOnChange(opacityChanges) { it.toDouble() }
    override var opacity by opacityProperty
}