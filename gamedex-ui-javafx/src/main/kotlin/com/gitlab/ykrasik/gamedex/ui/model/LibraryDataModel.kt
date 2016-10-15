package com.gitlab.ykrasik.gamedex.ui.model

import com.github.ykrasik.gamedex.datamodel.GamePlatform
import com.github.ykrasik.gamedex.datamodel.LibraryData
import tornadofx.getProperty
import tornadofx.property
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 12/10/2016
 * Time: 11:12
 */
class LibraryDataModel {
    var name by property<String>()
    fun nameProperty() = getProperty(LibraryDataModel::name)

    var path by property<String>()
    fun pathProperty() = getProperty(LibraryDataModel::path)

    var platform by property<GamePlatform>()
    fun platformProperty() = getProperty(LibraryDataModel::platform)

    val notAllFieldsSet = pathProperty().isNull.or(nameProperty().isNull).or(platformProperty().isNull)

    fun toLibraryData() = LibraryData(Paths.get(path), name, platform)

    override fun toString() = name
}