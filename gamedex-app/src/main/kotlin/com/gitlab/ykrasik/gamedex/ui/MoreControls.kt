package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.widgets.FixedRatingSkin
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewResizingPane
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXToggleButton
import com.jfoenix.controls.JFXToggleNode
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import javafx.util.Callback
import org.controlsfx.control.MaskerPane
import org.controlsfx.control.PopOver
import org.controlsfx.control.Rating
import tornadofx.*
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 09:33
 */
// TODO: Should probably replace with popoverComboMenu
inline fun <reified T : Enum<T>> EventTarget.enumComboBox(property: Property<T>? = null, noinline op: ComboBox<T>.() -> Unit = {}): ComboBox<T> {
    val enumValues = T::class.java.enumConstants.asList().observable<T>()
    return combobox(property, enumValues, op)
}

fun <S, T> TableView<S>.simpleColumn(title: String, valueProvider: (S) -> T): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { SimpleObjectProperty(valueProvider(it.value)) }
    columns.add(column)
    return column
}

inline fun <reified S> TableView<S>.customColumn(title: String,
                                                 crossinline cellFactory: (TableColumn<S, S>) -> TableCell<S, S>): TableColumn<S, S> {
    val column = TableColumn<S, S>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { SimpleObjectProperty(it.value) }
    column.setCellFactory { cellFactory(it) }
    return column
}

fun <S, T> TableView<S>.customColumn(title: String,
                                     valueProvider: (S) -> ObservableValue<T>,
                                     cellFactory: (TableColumn<S, T>) -> TableCell<S, T>): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { valueProvider(it.value) }
    column.setCellFactory { cellFactory(it) }
    return column
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

fun <S, T> TableView<S>.customGraphicColumn(title: String,
                                            valueProvider: (S) -> ObservableValue<T>,
                                            graphicFactory: (T) -> Node): TableColumn<S, T> =
    customColumn(title, valueProvider) {
        object : TableCell<S, T>() {
            override fun updateItem(item: T?, empty: Boolean) {
                graphic = item?.let { graphicFactory(it) }
            }
        }
    }

inline fun <reified S> TableView<S>.customGraphicColumn(title: String, crossinline graphicFactory: (S) -> Node): TableColumn<S, S> =
    customColumn(title) {
        object : TableCell<S, S>() {
            override fun updateItem(item: S?, empty: Boolean) {
                graphic = item?.let { graphicFactory(it) }
            }
        }
    }

// TODO: Can probably rewrite this using customGraphicColumn
inline fun <reified S> TableView<S>.imageViewColumn(title: String,
                                                    fitWidth: Double,
                                                    fitHeight: Double,
                                                    isPreserveRatio: Boolean = true,
                                                    crossinline imageRetriever: (S) -> ObservableValue<Image>): TableColumn<S, S> =
    customColumn(title) {
        object : TableCell<S, S>() {
            private val imageView = ImageView().apply {
                fadeOnImageChange()
                this@apply.fitHeight = fitHeight
                this@apply.fitWidth = fitWidth
                this@apply.isPreserveRatio = isPreserveRatio
            }

            init {
                addClass(CommonStyle.centered)
                graphic = imageView
            }

            override fun updateItem(item: S?, empty: Boolean) {
                if (item != null) {
                    val image = imageRetriever(item)
                    imageView.imageProperty().cleanBind(image)
                } else {
                    imageView.imageProperty().unbind()
                    imageView.image = null
                }
            }
        }
    }


fun EventTarget.fixedRating(max: Int, isPartial: Boolean = true, op: Rating.() -> Unit = {}) = opcr(this, Rating(max), op).apply {
    isPartialRating = isPartial
    skin = FixedRatingSkin(this)
}

fun EventTarget.imageViewResizingPane(imageView: ImageView, op: ImageViewResizingPane.() -> Unit = {}) =
    opcr(this, ImageViewResizingPane(imageView), op)

fun Node.clipRectangle(op: Rectangle.() -> Unit) {
    clip = Rectangle().apply(op)
}

fun EventTarget.jfxToggleButton(p: Property<Boolean>, op: (JFXToggleButton.() -> Unit)? = null) = jfxToggleButton {
    selectedProperty().bindBidirectional(p)
    op?.invoke(this)
}

fun EventTarget.jfxToggleButton(op: JFXToggleButton.() -> Unit = {}) = opcr(this, JFXToggleButton(), op)

fun Node.jfxToggleNode(graphic: Node? = null, group: ToggleGroup? = getToggleGroup(), op: JFXToggleNode.() -> Unit = {}) = opcr(this, JFXToggleNode().apply {
    addClass(CommonStyle.jfxHoverable)
    this.graphic = graphic
    this.toggleGroup = group
}, op)

fun Node.jfxToggleNode(text: String? = null,
                       graphic: Node? = null,
                       group: ToggleGroup? = getToggleGroup(),
                       labelStyleClasses: List<CssRule> = emptyList(),
                       op: JFXToggleNode.() -> Unit = {}): JFXToggleNode {
    val label = Label(text ?: "", graphic).apply {
        addClass(CommonStyle.jfxToggleNodeLabel, CommonStyle.fillAvailableWidth)
        labelStyleClasses.forEach { addClass(it) }
    }
    return jfxToggleNode(label, group, op)
}

fun EventTarget.jfxButton(text: String? = null, graphic: Node? = null, type: JFXButton.ButtonType = JFXButton.ButtonType.FLAT, op: JFXButton.() -> Unit = {}) =
    opcr(this, JFXButton().apply {
        addClass(CommonStyle.jfxHoverable)
        this.text = text
        this.graphic = graphic
        this.buttonType = type
    }, op)

// TODO: Change style classes to lists.
fun EventTarget.buttonWithPopover(text: String? = null,
                                  graphic: Node? = null,
                                  styleClass: CssRule? = CommonStyle.toolbarButton,
                                  arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                  closeOnClick: Boolean = true,
                                  op: (VBox.(PopOver) -> Unit)? = null) =
    jfxButton(text = text, graphic = graphic) {
        if (styleClass != null) addClass(styleClass)
        val popover = popOver(arrowLocation, closeOnClick, op)
        setOnAction { popover.toggle(this) }
    }

// TODO: Change style classes to lists.
fun <T> EventTarget.popoverComboMenu(possibleItems: List<T>,
                                     selectedItemProperty: Property<T>,
                                     arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                     styleClass: CssRule? = null,
                                     itemStyleClass: CssRule? = null,
                                     text: ((T) -> String)? = null,
                                     graphic: ((T) -> Node)? = null,
                                     menuOp: (VBox.(T) -> Unit)? = null) =
    buttonWithPopover(arrowLocation = arrowLocation, styleClass = styleClass) {
        possibleItems.forEach { item ->
            jfxButton(text?.invoke(item), graphic?.invoke(item)) {
                if (itemStyleClass != null) addClass(itemStyleClass)
                addClass(CommonStyle.fillAvailableWidth)
                setOnAction { selectedItemProperty.value = item }
            }
            menuOp?.invoke(this@buttonWithPopover, item)
        }
    }.apply {
        if (text != null) textProperty().bind(selectedItemProperty.map { text(it!!) })
        if (graphic != null) graphicProperty().bind(selectedItemProperty.map { graphic(it!!) })
    }

// TODO: Change style classes to lists.
fun <T> EventTarget.popoverComboMenu(possibleItems: ObservableList<T>,
                                     selectedItemProperty: Property<T>,
                                     arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                     styleClass: CssRule? = null,
                                     itemStyleClass: CssRule? = null,
                                     text: ((T) -> String)? = null,
                                     graphic: ((T) -> Node)? = null,
                                     menuOp: (VBox.(T) -> Unit)? = null) =
    buttonWithPopover(arrowLocation = arrowLocation, styleClass = styleClass) {
        possibleItems.performing { items ->
            replaceChildren {
                items.forEach { item ->
                    jfxButton(text?.invoke(item), graphic?.invoke(item)) {
                        if (itemStyleClass != null) addClass(itemStyleClass)
                        addClass(CommonStyle.fillAvailableWidth)
                        setOnAction { selectedItemProperty.value = item }
                    }
                    menuOp?.invoke(this@buttonWithPopover, item)
                }
            }
        }
    }.apply {
        if (text != null) textProperty().bind(selectedItemProperty.map { text(it!!) })
        if (graphic != null) graphicProperty().bind(selectedItemProperty.map { graphic(it!!) })
    }

// TODO: This smells like it should be a class
fun <T> Node.popoverToggleMenu(possibleItems: ObservableList<T>,
                               selectedItems: Property<List<T>>,
                               group: ToggleGroup? = getToggleGroup(),
                               arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                               styleClasses: List<CssRule> = emptyList(),
                               itemStyleClasses: List<CssRule> = emptyList(),
                               text: ((T) -> String)? = null,
                               graphic: ((T) -> Node)? = null,
                               menuOp: (VBox.(T) -> Unit)? = null) =
    buttonWithPopover(arrowLocation = arrowLocation, styleClass = null, closeOnClick = false) {
        toggleMenu(possibleItems, selectedItems, group, itemStyleClasses, text, graphic, menuOp)
    }.apply {
        styleClasses.forEach { addClass(it) }
        if (text != null) {
            textProperty().bind(selectedItems.map { selectedItems ->
                if (selectedItems!!.isEmpty() || selectedItems.toSet() == possibleItems.toSet()) "All"
                else selectedItems.map(text).sorted().joinToString(", ")
            })
        }
    }

fun <T> VBox.toggleMenu(possibleItems: ObservableList<T>,
                        selectedItems: Property<List<T>>,
                        group: ToggleGroup? = getToggleGroup(),
                        itemStyleClasses: List<CssRule> = emptyList(),
                        text: ((T) -> String)? = null,
                        graphic: ((T) -> Node)? = null,
                        menuOp: (VBox.(T) -> Unit)? = null) {
    val selectedItemsListeners = mutableListOf<ChangeListener<List<T>>>()
    possibleItems.performing { items ->
        selectedItemsListeners.forEach { selectedItems.removeListener(it) }
        selectedItemsListeners.clear()

        // TODO: Review all places where this is used for listener leaks.
        replaceChildren {
            items.forEach { item ->
                jfxToggleNode(text?.invoke(item), graphic?.invoke(item), group, itemStyleClasses) {
                    addClass(CommonStyle.fillAvailableWidth)
                    isSelected = selectedItems.value.contains(item)

                    selectedProperty().onChange {
                        selectedItems.value = if (it) (selectedItems.value + item).distinct() else selectedItems.value - item
                    }

                    selectedItemsListeners += selectedItems.changeListener {
                        isSelected = selectedItems.value.contains(item)
                    }
                }
                menuOp?.invoke(this@toggleMenu, item)
            }
        }
    }
}

fun popOver(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
            closeOnClick: Boolean = true,
            op: (VBox.(PopOver) -> Unit)? = null): PopOver = PopOver().apply {
    val popover = this
    this.arrowLocation = arrowLocation
    isAnimated = false  // A ton of exceptions start getting thrown if closing a window with an open popover without this.
    isDetachable = false

    val scrollpane = ScrollPane().apply {
        maxHeight = screenBounds.height * 3 / 4
        isFitToWidth = true
        isFitToHeight = true
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        if (closeOnClick) addEventFilter(MouseEvent.MOUSE_CLICKED) { popover.hide() }
        addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code === KeyCode.ESCAPE) popover.hide() }
    }
    scrollpane.content = VBox().apply {
        addClass(CommonStyle.popoverMenu)
        op?.invoke(this, popover)
    }
    contentNode = scrollpane
}

fun PopOver.replaceContent(content: Node) {
    (contentNode as ScrollPane).content = content
}

fun PopOver.toggle(parent: Node) = if (isShowing) hide() else show(parent)

fun <T> ListView<T>.fitAtMost(numItems: Int) {
    val size = itemsProperty().map { minOf(it!!.size, numItems) * 24.1 }
    minHeightProperty().bind(size)
    maxHeightProperty().bind(size)
}

fun StackPane.maskerPane(op: MaskerPane.() -> Unit = {}) = opcr(this, MaskerPane(), op)

inline fun View.skipFirstTime(op: () -> Unit) {
    val skip = properties.getOrDefault("Gamedex.skipFirstTime", true) as Boolean
    if (skip) {
        properties["Gamedex.skipFirstTime"] = false
    } else {
        op()
    }
}

fun Node.popoverContextMenu(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                            closeOnClick: Boolean = true,
                            op: (VBox.(PopOver) -> Unit)? = null): PopOver {
    val popover = popOver(arrowLocation, closeOnClick, op).apply { isAutoFix = false; isAutoHide = true }
    addEventHandler(MouseEvent.MOUSE_CLICKED) { popover.hide() }
    setOnContextMenuRequested { e -> popover.show(this@popoverContextMenu, e.screenX, e.screenY) }
    return popover
}

fun Node.dropDownMenu(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                      closeOnClick: Boolean = true,
                      op: (VBox.() -> Unit)? = null): PopOver {
    var popoverHack: PopOver? = null
    val popover = popOver(arrowLocation, closeOnClick) {
        addEventHandler(MouseEvent.MOUSE_EXITED) { popoverHack!!.hide() }
        op?.invoke(this)
    }
    popoverHack = popover

    addEventHandler(MouseEvent.MOUSE_ENTERED) {
        if (!popover.isShowing) popover.show(this@dropDownMenu)
    }
    setOnMouseExited {
        if (!(it.screenX >= popover.x && it.screenX <= popover.x + popover.width &&
                it.screenY >= popover.y && it.screenY <= popover.y + popover.height)) {
            popover.hide()
        }
    }
    return popover
}

fun ToggleGroup.disallowDeselection() {
    selectedToggleProperty().addListener { _, oldValue, newValue ->
        if (newValue == null) {
            selectToggle(oldValue)
        }
    }
}