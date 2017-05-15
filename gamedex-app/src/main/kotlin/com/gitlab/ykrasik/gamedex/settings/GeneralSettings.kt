package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.util.LogLevel
import javafx.beans.property.ObjectProperty
import tornadofx.getValue
import tornadofx.setValue
import java.io.File

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
class GeneralSettings private constructor() : AbstractSettings("general") {
    companion object {
        operator fun invoke(): GeneralSettings = readOrUse(GeneralSettings())
    }

    @Transient
    val prevDirectoryProperty: ObjectProperty<File?> = preferenceProperty(null)
    var prevDirectory by prevDirectoryProperty

    @Transient
    val logFilterLevelProperty = preferenceProperty(LogLevel.info)
    var logFilterLevel by logFilterLevelProperty

    @Transient
    val logTailProperty = preferenceProperty(true)
    var logTail by logTailProperty

    @Transient
    val amountOfDiComponentsProperty = preferenceProperty(25)
    var amountOfDiComponents by amountOfDiComponentsProperty
}