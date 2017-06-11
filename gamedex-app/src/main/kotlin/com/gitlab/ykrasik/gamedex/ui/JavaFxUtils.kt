package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.ui.theme.Theme.Images
import com.gitlab.ykrasik.gamedex.ui.theme.acceptButton
import com.gitlab.ykrasik.gamedex.ui.theme.cancelButton
import javafx.application.Platform.runLater
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage
import javafx.stage.StageStyle
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

class ThreadAwareObjectProperty<T> : SimpleObjectProperty<T>() {
    override fun fireValueChangedEvent() {
        runLaterIfNecessary { super.fireValueChangedEvent() }
    }
}

class ThreadAwareStringProperty(initialValue: String? = null) : SimpleStringProperty(initialValue) {
    override fun fireValueChangedEvent() {
        runLaterIfNecessary { super.fireValueChangedEvent() }
    }
}

class ThreadAwareDoubleProperty(initialValue: Double = 0.0) : SimpleDoubleProperty(initialValue) {
    override fun fireValueChangedEvent() {
        runLaterIfNecessary { super.fireValueChangedEvent() }
    }
}

class ThreadAwareBooleanProperty(initialValue: Boolean = false) : SimpleBooleanProperty(initialValue) {
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

fun areYouSureDialog(text: String = "Are You Sure?", op: (VBox.() -> Unit)? = null): Boolean = object : Fragment(text) {
    private var accept = false

    override val root = borderpane {
        minWidth = 400.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton {
                    setOnAction { close(accept = true) }
                }
                spacer()
                cancelButton {
                    isCancelButton = true
                    setOnAction { close(accept = false) }
                }
            }
        }
        center {
            vbox(spacing = 10.0) {
                hbox {
                    paddingAll = 20.0
                    alignment = Pos.CENTER_LEFT
                    label(text)
                    spacer()
                    imageview(Images.warning)
                }
                if (op != null) {
                    separator()
                    vbox(spacing = 10.0) {
                        paddingAll = 20.0
                        paddingRight = 30.0
                        op(this)
                    }
                }
            }
        }
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    fun show(): Boolean {
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
        return accept
    }
}.show()

fun infoAlert(title: String = "Are You Sure?", op: (VBox.() -> Unit)? = null) = object : Fragment(title) {
    override val root = borderpane {
        minWidth = 400.0
        minHeight = 100.0
        top {
            toolbar {
                acceptButton { setOnAction { close() } }
            }
        }
        center {
            vbox(spacing = 10.0) {
                hbox {
                    paddingAll = 20.0
                    alignment = Pos.CENTER_LEFT
                    label(title) {
                        font = Font.font(16.0)
                    }
                    spacer()
                    imageview(Images.information)
                }
                if (op != null) {
                    separator()
                    vbox(spacing = 10.0) {
                        paddingAll = 20.0
                        paddingRight = 30.0
                        op(this)
                    }
                }
            }
        }
    }

    fun show() {
        openModal(block = true, stageStyle = StageStyle.UNIFIED)
    }
}.show()

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

fun FontAwesome.Glyph.toGraphic(op: (Glyph.() -> Unit)? = null) = Glyph("FontAwesome", this).apply {
    op?.invoke(this)
}

fun EventTarget.imageview(image: Image, op: (ImageView.() -> Unit)? = null) = opcr(this, ImageView(image), op)
fun EventTarget.imageview(imageProperty: ObservableValue<Image>, op: (ImageView.() -> Unit)? = null) = opcr(this, ImageView().apply {
    imageProperty().bind(imageProperty)
}, op)

inline fun <reified T : Number> EventTarget.textfield(property: ObservableValue<T>, noinline op: (TextField.() -> Unit)? = null) = textfield().apply {
    bind(property)
    op?.invoke(this)
}

fun <S> TableView<S>.allowDeselection(onClickAgain: Boolean) {
    val tableView = this
    var lastSelectedRow: TableRow<S>? = null
    setRowFactory {
        TableRow<S>().apply {
            selectedProperty().onChange {
                if (it) lastSelectedRow = this
            }
            if (onClickAgain) {
                addEventFilter(MouseEvent.MOUSE_PRESSED) { e ->
                    if (index >= 0 && index < tableView.items.size && tableView.selectionModel.isSelected(index)) {
                        tableView.selectionModel.clearSelection()
                        e.consume()
                    }
                }
            }
        }
    }
    addEventFilter(MouseEvent.MOUSE_CLICKED) { e ->
        lastSelectedRow?.let { lastSelectedRow ->
            val boundsOfSelectedRow = lastSelectedRow.localToScene(lastSelectedRow.layoutBounds)
            if (!boundsOfSelectedRow.contains(e.sceneX, e.sceneY)) {
                tableView.selectionModel.clearSelection()
            }
        }
    }
}

fun Node.showWhen(expr: () -> ObservableValue<Boolean>) {
    val shouldShow = expr()
    managedWhen { shouldShow }
    visibleWhen { shouldShow }
}

fun Node.mouseTransparentWhen(expr: () -> ObservableValue<Boolean>) {
    mouseTransparentProperty().cleanBind(expr())
}