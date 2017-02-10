package com.gitlab.ykrasik.gamedex.model

import com.gitlab.ykrasik.gamedex.common.datamodel.GamePlatform
import com.gitlab.ykrasik.gamedex.common.datamodel.LibraryData
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.persistence.AddLibraryRequest
import tornadofx.getProperty
import tornadofx.property

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

    fun toRequest() = AddLibraryRequest(path = path.toFile(), data = LibraryData(platform, name))

    override fun toString() = name
}