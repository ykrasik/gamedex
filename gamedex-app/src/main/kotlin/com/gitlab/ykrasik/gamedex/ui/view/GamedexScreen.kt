package com.gitlab.ykrasik.gamedex.ui.view

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.ToolBar
import tornadofx.View

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 15:50
 */
abstract class GamedexScreen(title: String) : View(title) {
    abstract fun ToolBar.constructToolbar()

    open val useDefaultNavigationButton: Boolean = true

    // Yuck
    val closeRequestedProperty = SimpleBooleanProperty(false)
}