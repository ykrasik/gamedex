package com.gitlab.ykrasik.gamedex.core.ui.model

import com.github.ykrasik.gamedex.datamodel.ExcludedPath
import com.gitlab.ykrasik.gamedex.persistence.PersistenceService
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.getValue
import tornadofx.observable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 25/12/2016
 * Time: 20:12
 */
@Singleton
class ExcludedPathRepository @Inject constructor(
    private val persistenceService: PersistenceService
) {
    val allProperty: ListProperty<ExcludedPath> = SimpleListProperty(persistenceService.excludedPaths.all.observable())
    val all: ObservableList<ExcludedPath> by allProperty

    fun contains(path: File): Boolean = all.any { it.path == path }

    fun add() {
        TODO()  // TODO: Implement
    }

    fun delete() {
        TODO()  // TODO: Implement
    }
}