package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ui.widgets.FixedRatingSkin
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewResizingPane
import com.jfoenix.controls.*
import javafx.application.Platform.runLater
import javafx.beans.binding.Bindings
import javafx.beans.binding.ListExpression
import javafx.beans.binding.NumberBinding
import javafx.beans.property.*
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.TransformationList
import javafx.event.EventTarget
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.Duration
import org.controlsfx.control.PopOver
import org.controlsfx.control.Rating
import org.controlsfx.control.StatusBar
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import java.io.ByteArrayInputStream
import java.util.*
import java.util.function.Predicate
import kotlin.reflect.KProperty1

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

fun <T> ObservableList<T>.unmodifiable(): ObservableList<T> = FXCollections.unmodifiableObservableList(this)
fun <T> ObservableList<T>.sizeProperty(): ReadOnlyIntegerProperty {
    val p = SimpleIntegerProperty(this.size)
    this.addListener(ListChangeListener { p.set(this.size) })
    return p
}

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

fun Region.printSize(id: String) { printWidth(id); printHeight(id) }
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

fun <T> ObservableValue<T>.printChanges(id: String) = addListener { _, o, v -> println("$id changed: $o -> $v") }

fun <T, R> ListExpression<T>.mapped(f: (T) -> R): ListProperty<R> = SimpleListProperty(this.value.mapped(f))
fun <T, R> ObservableList<T>.mapped(f: (T) -> R): ObservableList<R> = MappedList(this, f)

// TODO: This is the un-optimized version
fun <T, R> ObservableList<T>.flatMapped(f: (T) -> List<R>): ObservableList<R> {
    fun doFlatMap() = this.flatMap(f)

    val list = FXCollections.observableArrayList(doFlatMap())
    this.onChange {
        list.setAll(doFlatMap())
    }
    return list
}

// TODO: This is the un-optimized version
fun <T> ObservableList<T>.distincted(): ObservableList<T> {
    fun doDistinct() = this.distinct()

    val list = FXCollections.observableArrayList(doDistinct())
    this.onChange {
        list.setAll(doDistinct())
    }
    return list
}

/**
 * Creates a new MappedList list wrapped around the source list.
 * Each element will have the given function applied to it, such that the list is cast through the mapper.
 * Taken from https://gist.github.com/mikehearn/a2e4a048a996fd900656
 */
class MappedList<E, F>(source: ObservableList<out F>, private val mapper: (F) -> E) : TransformationList<E, F>(source) {
    private var mapped = transform()

    private fun transform(): MutableList<E> = source.map(mapper) as MutableList<E>

    override fun sourceChanged(c: ListChangeListener.Change<out F>) {
        // Is all this stuff right for every case? Probably it doesn't matter for this app.
        beginChange()
        while (c.next()) {
            if (c.wasPermutated()) {
                val perm = IntArray(c.to - c.from)
                for (i in c.from..c.to - 1)
                    perm[i - c.from] = c.getPermutation(i)
                nextPermutation(c.from, c.to, perm)
            } else if (c.wasUpdated()) {
                for (i in c.from..c.to - 1) {
                    remapIndex(i)
                    nextUpdate(i)
                }
            } else {
                if (c.wasRemoved()) {
                    // Removed should come first to properly handle replacements, then add.
                    val removed = mapped.subList(c.from, c.from + c.removedSize)
                    val duped = ArrayList(removed)
                    removed.clear()
                    nextRemove(c.from, duped)
                }
                if (c.wasAdded()) {
                    for (i in c.from..c.to - 1) {
                        mapped.addAll(c.from, c.addedSubList.map(mapper))
                        remapIndex(i)
                    }
                    nextAdd(c.from, c.to)
                }
            }
        }
        endChange()
    }

    private fun remapIndex(i: Int) {
        if (i >= mapped.size) {
            for (j in mapped.size..i) {
                mapped.add(mapper(source[j]))
            }
        }
        mapped[i] = mapper(source[i])
    }

    override fun getSourceIndex(index: Int) = index

    override fun get(index: Int): E = mapped[index]

    override val size: Int get() = mapped.size
}

fun <T, R> ObservableValue<T>.mapProperty(f: (T?) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this.value))
    this.onChange { property.set(f(it)) }
    return property
}

fun <T, R> ObservableValue<T>.toPredicate(f: (T?, R) -> Boolean): Property<Predicate<R>> =
    mapProperty { t -> Predicate { r: R -> f(t, r) } }

fun <T> ObservableValue<Predicate<T>>.and(other: ObservableValue<Predicate<T>>): Property<Predicate<T>> {
    fun compose() = this.value.and(other.value)
    val property = SimpleObjectProperty(compose())
    this.onChange { property.set(compose()) }
    other.onChange { property.set(compose()) }
    return property
}

fun <T, R> ObservableList<T>.mapProperty(f: (ObservableList<T>) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this))
    this.onChange {
        property.set(f(this))
    }
    return property
}

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

fun EventTarget.fixedRating(max: Int, isPartial: Boolean = true, op: (Rating.() -> Unit)? = null) = opcr(this, Rating(max), op).apply {
    isPartialRating = isPartial
    skin = FixedRatingSkin(this)
}

fun EventTarget.imageViewResizingPane(imageView: ImageView, op: (ImageViewResizingPane.() -> Unit)? = null) =
    opcr(this, ImageViewResizingPane(imageView), op)

fun Node.clipRectangle(op: Rectangle.() -> Unit) {
    clip = Rectangle().apply(op)
}

fun ObservableNumberValue.min(other: ObservableNumberValue): NumberBinding = Bindings.min(this, other)

fun EventTarget.statusBar(op: (StatusBar.() -> Unit)? = null) = opcr(this, StatusBar(), op)
fun StatusBar.left(op: (Node.() -> Unit)) = statusBarItems(op, leftItems)
fun StatusBar.right(op: (Node.() -> Unit)) = statusBarItems(op, rightItems)
private fun statusBarItems(op: (Node.() -> Unit), items: ObservableList<Node>) {
    val target = object : Group() {
        override fun getChildren() = items
    }
    op(target)
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

fun EventTarget.jfxHamburger(op: (JFXHamburger.() -> Unit)? = null) = opcr(this, JFXHamburger(), op)
fun EventTarget.jfxDrawer(op: (JFXDrawer.() -> Unit)? = null) = opcr(this, JFXDrawer(), op)
fun EventTarget.jfxToggleButton(op: (JFXToggleButton.() -> Unit)? = null) = opcr(this, JFXToggleButton(), op)
fun EventTarget.jfxToggleNode(graphic: Node? = null, op: (JFXToggleNode.() -> Unit)? = null) = opcr(this, JFXToggleNode().apply {
    this.graphic = graphic
}, op)
fun EventTarget.jfxButton(text: String? = null, graphic: Node? = null, type: JFXButton.ButtonType = JFXButton.ButtonType.FLAT, op: (JFXButton.() -> Unit)? = null) =
    opcr(this, JFXButton().apply {
        addClass(CommonStyle.jfxButton)
        this.text = text
        this.graphic = graphic
        this.buttonType = type
    }, op)
fun <T> EventTarget.jfxComboBox(property: Property<T>? = null, values: List<T>? = null, op: (JFXComboBox<T>.() -> Unit)? = null) = opcr(this, JFXComboBox<T>().apply {
    if (values != null) items = if (values is ObservableList<*>) values as ObservableList<T> else values.observable()
    if (property != null) valueProperty().bindBidirectional(property)
}, op)

fun popOver(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT, op: (PopOver.() -> Unit)? = null): PopOver =
    PopOver().apply {
        this.arrowLocation = arrowLocation
        op?.invoke(this)
    }

fun Button.withPopover(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT, op: (PopOver.() -> Unit)? = null) {
    val popover = popOver(arrowLocation, op)
    setOnAction {
        if (popover.isShowing) popover.hide() else popover.show(this)
    }
}

inline fun <reified T : Number> EventTarget.textfield(property: ObservableValue<T>, noinline op: (TextField.() -> Unit)? = null) = textfield().apply {
    bind(property)
    op?.invoke(this)
}

fun EventTarget.platformComboBox(property: Property<Platform>? = null,
                                 values: List<Platform>? = null,
                                 op: (ComboBox<Platform>.() -> Unit)? = null) {
    combobox(property, values) {
        setCellFactory {
            object : ListCell<Platform>() {
                override fun updateItem(item: Platform?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item == null || empty) {
                        text = null
                        graphic = null
                    } else {
                        text = item.toString()
                        graphic = item.toLogo()
                    }
                }
            }
        }
        buttonCell = object : ListCell<Platform>() {
            override fun updateItem(item: Platform?, empty: Boolean) {
                super.updateItem(item, empty)
                if (item == null || empty) {
                    text = null
                    graphic = null
                } else {
                    text = item.toString()
                    graphic = item.toLogo()
                }
            }
        }

        op?.invoke(this)
    }
}

fun Platform.toLogo() = when (this) {
    Platform.pc -> FontAwesome.Glyph.WINDOWS.toGraphic { color(Color.CORNFLOWERBLUE); size(19.0) }
    Platform.android -> FontAwesome.Glyph.ANDROID.toGraphic { color(Color.FORESTGREEN); size(19.0) }
    Platform.mac -> FontAwesome.Glyph.APPLE.toGraphic { color(Color.GRAY); size(19.0) }
    else -> FontAwesome.Glyph.QUESTION.toGraphic { size(19.0) }
}