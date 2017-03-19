package com.gitlab.ykrasik.gamedex.ui

import javafx.beans.property.Property
import javafx.event.EventTarget
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.util.Callback
import javafx.util.Duration
import tornadofx.*
import kotlin.reflect.KProperty1

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

inline fun <reified S, T> TableView<S>.customColumn(title: String,
                                                    prop: KProperty1<S, T>,
                                                    crossinline cellFactory: (TableColumn<S, T>) -> TableCell<S, T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    column.setCellFactory { cellFactory(it) }
    return column
}

fun areYouSureDialog(textBody: String? = null, op: (Alert.() -> Unit)? = null): Boolean {
    // TODO: TornadoFx has a built-in 'confirm' method.
    val alert = Alert(Alert.AlertType.CONFIRMATION, textBody ?: "Are You Sure?", ButtonType.CANCEL, ButtonType.OK)
    alert.headerText = "Are You Sure?"
    op?.invoke(alert)
    val buttonClicked = alert.showAndWait()

    var ok = false
    buttonClicked.ifPresent {
        when (it) {
            ButtonType.OK -> ok = true
            ButtonType.CANCEL -> ok = false
            else -> error("Unexpected buttonType: $it")
        }
    }
    return ok
}

fun ButtonBar.okButton(op: (Button.() -> Unit)? = null): Button {
    return button("OK", type = ButtonBar.ButtonData.OK_DONE) {
        op?.invoke(this)
        isDefaultButton = true
    }
}

fun ButtonBar.cancelButton(op: (Button.() -> Unit)? = null): Button {
    return button("Cancel", type = ButtonBar.ButtonData.LEFT) {
        op?.invoke(this)
        isCancelButton = true
    }
}

fun ImageView.fadeOnImageChange(fadeInDuration: Duration = 0.2.seconds): ImageView {
    imageProperty().onChange {
        fade(fadeInDuration, 1.0, play = true) {
            fromValue = 0.0
        }
    }
    return this
}