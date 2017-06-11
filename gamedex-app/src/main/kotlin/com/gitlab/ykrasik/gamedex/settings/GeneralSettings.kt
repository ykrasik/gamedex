package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.util.LogLevel
import tornadofx.getValue
import tornadofx.setValue
import java.io.File

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
class GeneralSettings : SettingsScope() {
    @Transient
    val prevDirectoryProperty = preferenceProperty<File?>(null)
    var prevDirectory by prevDirectoryProperty

    @Transient
    val exportDbDirectoryProperty = preferenceProperty<File?>(null)
    var exportDbDirectory by exportDbDirectoryProperty

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