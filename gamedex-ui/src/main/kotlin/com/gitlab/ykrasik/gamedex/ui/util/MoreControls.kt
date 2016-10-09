package com.gitlab.ykrasik.gamedex.ui.util

import javafx.event.EventTarget
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import tornadofx.tab
import tornadofx.textarea
import tornadofx.textfield

/**
 * User: ykrasik
 * Date: 09/10/2016
 * Time: 16:10
 */
fun EventTarget.readOnlyTextField(value: String? = null, op: (TextField.() -> Unit)? = null) = textfield(value, op).apply {
    isEditable = false
}

fun EventTarget.readOnlyTextArea(value: String? = null, op: (TextArea.() -> Unit)? = null) = textarea(value, op).apply {
    isEditable = false
}

fun TabPane.nonClosableTab(text: String, op: (Tab.() -> Unit)? = null) = tab(text, op).apply {
    isClosable = false
}