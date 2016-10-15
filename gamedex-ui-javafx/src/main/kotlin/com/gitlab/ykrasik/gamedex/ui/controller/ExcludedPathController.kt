package com.gitlab.ykrasik.gamedex.ui.controller

import com.gitlab.ykrasik.gamedex.core.ui.ExcludedPathUIManager
import com.gitlab.ykrasik.gamedex.persistence.dao.ExcludedPathDao
import javafx.beans.property.SimpleListProperty
import tornadofx.getValue
import tornadofx.observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 10/10/2016
 * Time: 13:32
 */
@Singleton
class ExcludedPathController @Inject constructor(
    private val excludedPathDao: ExcludedPathDao
): ExcludedPathUIManager {
    val allProperty = SimpleListProperty(excludedPathDao.all.observable())
    override val all by allProperty

    fun add() {
        TODO()  // TODO: Implement
    }

    fun delete() {
        TODO()  // TODO: Implement
    }
}