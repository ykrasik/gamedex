package com.gitlab.ykrasik.gamedex.ui

import javafx.application.Platform.runLater
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.stage.Stage
import javafx.util.Duration
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import java.io.ByteArrayInputStream

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 20:45
 */
fun runLaterIfNecessary(f: () -> Unit) = if (javafx.application.Platform.isFxApplicationThread()) {
    f()
} else {
    runLater(f)
}

fun ByteArray.toImage(): Image = Image(ByteArrayInputStream(this))
fun ByteArray.toImageView(): ImageView = this.toImage().toImageView()
fun Image.toImageView(): ImageView = ImageView(this)

fun <S> TableView<S>.clearSelection() = selectionModel.clearSelection()

class ThreadAwareStringProperty : SimpleStringProperty() {
    override fun fireValueChangedEvent() {
        runLaterIfNecessary { super.fireValueChangedEvent() }
    }
}

class ThreadAwareDoubleProperty : SimpleDoubleProperty() {
    override fun fireValueChangedEvent() {
        runLaterIfNecessary { super.fireValueChangedEvent() }
    }
}

fun Region.printSize(id: String) {
    printWidth(id)
    printHeight(id)
}

fun Region.printWidth(id: String) = printSize(id, "width", minWidthProperty(), maxWidthProperty(), prefWidthProperty(), widthProperty())
fun Region.printHeight(id: String) = printSize(id, "height", minHeightProperty(), maxHeightProperty(), prefHeightProperty(), heightProperty())

fun <S, T> TableColumnBase<S, T>.printWidth(id: String) {
    printSize(id, "width", minWidthProperty(), maxWidthProperty(), prefWidthProperty(), widthProperty())
}

fun Stage.printWidth(id: String) {
    printSize(id, "width", minWidthProperty(), maxWidthProperty(), null, widthProperty())
}

private fun printSize(id: String,
                      sizeName: String,
                      min: ObservableValue<Number>,
                      max: ObservableValue<Number>,
                      pref: ObservableValue<Number>?,
                      actual: ObservableValue<Number>) {
    println("$id min-$sizeName = ${min.value}")
    println("$id max-$sizeName = ${max.value}")
    pref?.let { println("$id pref-$sizeName = ${it.value}") }
    println("$id $sizeName = ${actual.value}")

    min.printChanges("$id min-$sizeName")
    max.printChanges("$id max-$sizeName")
    pref?.printChanges("$id pref-$sizeName")
    actual.printChanges("$id $sizeName")
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

fun Region.padding(op: (InsetBuilder.() -> Unit)) {
    val builder = InsetBuilder(this)
    op(builder)
    padding = Insets(builder.top.toDouble(), builder.right.toDouble(), builder.bottom.toDouble(), builder.left.toDouble())
}

class InsetBuilder(region: Region) {
    var top: Number = region.padding.top
    var bottom: Number = region.padding.bottom
    var right: Number = region.padding.right
    var left: Number = region.padding.left
}

fun EventTarget.verticalSeparator(padding: Double? = 10.0, op: (Separator.() -> Unit)? = null) = separator(Orientation.VERTICAL, op).apply {
    padding?.let {
        padding { right = it; left = it }
    }
}

var SplitPane.dividerPosition: Double
    get() = dividerPositions.first()
    set(value) = setDividerPositions(value)

fun FontAwesome.Glyph.toGraphic(op: (Glyph.() -> Unit)? = null) = Glyph("FontAwesome", this).apply {
    op?.invoke(this)
}

fun EventTarget.imageview(image: Image, op: (ImageView.() -> Unit)? = null) = opcr(this, ImageView(image), op)

inline fun <reified T : Number> EventTarget.textfield(property: ObservableValue<T>, noinline op: (TextField.() -> Unit)? = null) = textfield().apply {
    bind(property)
    op?.invoke(this)
}