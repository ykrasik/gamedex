package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.ExcludedPath
import com.gitlab.ykrasik.gamedex.persistence.dao.ExcludedPathDao
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.getValue
import tornadofx.observable
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 20:12
 */
@Singleton
class ExcludedPathsModel @Inject constructor(
    private val excludedPathDao: ExcludedPathDao
) {
    val allProperty = SimpleListProperty(excludedPathDao.all.observable())
    val all: ObservableList<ExcludedPath> by allProperty

    fun contains(path: Path): Boolean = all.any { it.path == path }

    fun add() {
        TODO()  // TODO: Implement
    }

    fun delete() {
        TODO()  // TODO: Implement
    }
}