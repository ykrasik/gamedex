package com.gitlab.ykrasik.gamedex.ui.util

import javafx.beans.property.Property
import javafx.event.EventTarget
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import javafx.stage.Window
import tornadofx.*
import java.nio.file.Path
import java.nio.file.Paths

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

inline fun <reified T : Enum<T>> EventTarget.enumComboBox(property: Property<T>? = null, noinline op: (ComboBox<T>.() -> Unit)? = null): ComboBox<T> {
    val enumValues = T::class.java.enumConstants.asList().observable<T>()
    return combobox(property, enumValues, op)
}

fun chooseDirectory(title: String? = null, owner: Window? = null, op: (DirectoryChooser.() -> Unit)? = null): Path? {
    val chooser = DirectoryChooser()
    if (title != null) chooser.title = title
    op?.invoke(chooser)
    val result = chooser.showDialog(owner)
    return result?.let { Paths.get(it.toURI()) }
}