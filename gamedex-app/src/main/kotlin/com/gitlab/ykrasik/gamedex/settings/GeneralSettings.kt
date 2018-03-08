package com.gitlab.ykrasik.gamedex.settings

import ch.qos.logback.classic.Level
import tornadofx.getValue
import tornadofx.setValue
import java.io.File
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:10
 */
@Singleton
class GeneralSettings {
    private val repo = SettingsRepo("general") { Data() }

    val prevDirectoryProperty = repo.property(Data::prevDirectory) { copy(prevDirectory = it) }
    var prevDirectory by prevDirectoryProperty

    val exportDbDirectoryProperty = repo.property(Data::exportDbDirectory) { copy(exportDbDirectory = it) }
    var exportDbDirectory by exportDbDirectoryProperty

    val logFilterLevelProperty = repo.stringProperty(Data::logFilterLevel) { copy(logFilterLevel = it) }
    var logFilterLevel by logFilterLevelProperty

    val logTailProperty = repo.booleanProperty(Data::logTail) { copy(logTail = it) }
    var logTail by logTailProperty

    data class Data(
        val prevDirectory: File? = null,
        val exportDbDirectory: File? = null,
        val logFilterLevel: String = Level.INFO.levelStr,
        val logTail: Boolean = true
    )
}