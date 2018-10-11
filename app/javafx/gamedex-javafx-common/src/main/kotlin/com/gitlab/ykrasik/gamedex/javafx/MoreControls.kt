/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.javafx

import com.gitlab.ykrasik.gamedex.javafx.control.FixedRatingSkin
import com.gitlab.ykrasik.gamedex.javafx.control.ImageViewResizingPane
import com.jfoenix.controls.*
import javafx.animation.FadeTransition
import javafx.beans.property.BooleanProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Pos
import javafx.scene.Cursor
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
import javafx.util.Duration
import org.controlsfx.control.MaskerPane
import org.controlsfx.control.PopOver
import org.controlsfx.control.Rating
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 09:33
 */
fun <S, T> TableView<S>.simpleColumn(title: String, valueProvider: (S) -> T): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    column.cellValueFactory = Callback { SimpleObjectProperty(valueProvider(it.value)) }
    columns.add(column)
    return column
}

inline fun <reified S> TableView<S>.customColumn(
    title: String,
    crossinline cellFactory: (TableColumn<S, S>) -> TableCell<S, S>
): TableColumn<S, S> {
    val column = TableColumn<S, S>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { SimpleObjectProperty(it.value) }
    column.setCellFactory { cellFactory(it) }
    return column
}

fun <S, T> TableView<S>.customColumn(
    title: String,
    valueProvider: (S) -> ObservableValue<T>,
    cellFactory: (TableColumn<S, T>) -> TableCell<S, T>
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { valueProvider(it.value) }
    column.setCellFactory { cellFactory(it) }
    return column
}

inline fun <reified S, T> TableView<S>.customColumn(
    title: String,
    prop: KProperty1<S, T>,
    crossinline cellFactory: (TableColumn<S, T>) -> TableCell<S, T>
): TableColumn<S, T> {
    val column = TableColumn<S, T>(title)
    addColumnInternal(column)
    column.cellValueFactory = Callback { observable(it.value, prop) }
    column.setCellFactory { cellFactory(it) }
    return column
}

fun <S, T> TableView<S>.customGraphicColumn(
    title: String,
    valueProvider: (S) -> ObservableValue<T>,
    graphicFactory: (T) -> Node
): TableColumn<S, T> = customColumn(title, valueProvider) {
    object : TableCell<S, T>() {
        override fun updateItem(item: T?, empty: Boolean) {
            graphic = item?.let { graphicFactory(it) }
        }
    }
}

inline fun <reified S> TableView<S>.customGraphicColumn(title: String, crossinline graphicFactory: (S) -> Node): TableColumn<S, S> =
    customColumn(title) {
        object : TableCell<S, S>() {
            init {
                alignment = Pos.CENTER
            }

            override fun updateItem(item: S?, empty: Boolean) {
                graphic = item?.let { graphicFactory(it) }
            }
        }
    }

// TODO: Can probably rewrite this using customGraphicColumn
inline fun <reified S> TableView<S>.imageViewColumn(
    title: String,
    fitWidth: Double,
    fitHeight: Double,
    isPreserveRatio: Boolean = true,
    crossinline imageRetriever: (S) -> ObservableValue<Image>
): TableColumn<S, S> = customColumn(title) {
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


inline fun EventTarget.fixedRating(max: Int, isPartial: Boolean = true, op: Rating.() -> Unit = {}) = opcr(this, Rating(max), op).apply {
    isPartialRating = isPartial
    skin = FixedRatingSkin(this)
}

inline fun EventTarget.imageViewResizingPane(imageView: ImageView, op: ImageViewResizingPane.() -> Unit = {}) =
    opcr(this, ImageViewResizingPane(imageView), op)

inline fun EventTarget.imageViewResizingPane(image: ObservableValue<Image>, op: ImageViewResizingPane.() -> Unit = {}) =
    imageViewResizingPane(ImageView()) {
        imageProperty.bind(image)
        op()
    }

inline fun Node.clipRectangle(op: Rectangle.() -> Unit) {
    clip = Rectangle().apply(op)
}

inline fun EventTarget.jfxToggleButton(p: Property<Boolean>, text: String? = null, crossinline op: JFXToggleButton.() -> Unit = {}) = jfxToggleButton {
    selectedProperty().bindBidirectional(p)
    this.text = text
    op(this)
}

inline fun EventTarget.jfxToggleButton(op: JFXToggleButton.() -> Unit = {}) = opcr(this, JFXToggleButton(), op)

inline fun EventTarget.jfxCheckBox(p: Property<Boolean>, text: String? = null, crossinline op: JFXCheckBox.() -> Unit = {}) = jfxCheckBox {
    selectedProperty().bindBidirectional(p)
    this.text = text
    op(this)
}

inline fun EventTarget.jfxCheckBox(op: JFXCheckBox.() -> Unit = {}) = opcr(this, JFXCheckBox(), op)

inline fun Node.jfxToggleNode(graphic: Node? = null, group: ToggleGroup? = getToggleGroup(), op: JFXToggleNode.() -> Unit = {}) = opcr(this, JFXToggleNode().apply {
    addClass(CommonStyle.jfxHoverable)
    this.graphic = graphic
    this.toggleGroup = group
}, op)

inline fun Node.jfxToggleNode(
    text: String? = null,
    graphic: Node? = null,
    value: Any? = null,
    group: ToggleGroup? = getToggleGroup(),
    labelStyleClasses: List<CssRule> = emptyList(),
    op: JFXToggleNode.() -> Unit = {}
): JFXToggleNode {
    val actualText = if (value != null && text == null) value.toString() else text ?: ""
    val label = Label(actualText, graphic).apply {
        addClass(CommonStyle.jfxToggleNodeLabel)
        useMaxWidth = true
        labelStyleClasses.forEach { addClass(it) }
    }
    return jfxToggleNode(label, group, op).apply {
        properties["tornadofx.toggleGroupValue"] = value ?: text
    }
}

inline fun EventTarget.jfxButton(text: String? = null, graphic: Node? = null, type: JFXButton.ButtonType = JFXButton.ButtonType.FLAT, op: JFXButton.() -> Unit = {}) =
    opcr(this, JFXButton().apply {
        addClass(CommonStyle.jfxHoverable)
        this.text = text
        this.graphic = graphic
        this.buttonType = type
    }, op)

// TODO: Change style classes to lists.
inline fun EventTarget.buttonWithPopover(
    text: String? = null,
    graphic: Node? = null,
    styleClass: CssRule? = CommonStyle.toolbarButton,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.(PopOver) -> Unit = {}
) = jfxButton(text = text, graphic = graphic) {
    if (styleClass != null) addClass(styleClass)
    val popover = popOver(arrowLocation, closeOnClick, op)
    setOnAction { popover.toggle(this) }
}

inline fun <reified T : Enum<T>> EventTarget.enumComboMenu(
    selectedItemProperty: Property<T>,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    styleClass: CssRule? = null,
    itemStyleClass: CssRule? = null,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node)? = null,
    menuOp: VBox.(T) -> Unit = {}
) = popoverComboMenu(
    possibleItems = T::class.java.enumConstants.asList(),
    selectedItemProperty = selectedItemProperty,
    arrowLocation = arrowLocation,
    styleClass = styleClass,
    itemStyleClass = itemStyleClass,
    text = text,
    graphic = graphic,
    menuOp = menuOp
)

// TODO: Change style classes to lists.
inline fun <T> EventTarget.popoverComboMenu(
    possibleItems: List<T>,
    selectedItemProperty: Property<T>,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    styleClass: CssRule? = null,
    itemStyleClass: CssRule? = null,
    noinline text: ((T) -> String)? = Any?::toString,
    noinline graphic: ((T) -> Node)? = null,
    menuOp: VBox.(T) -> Unit = {}
) = buttonWithPopover(arrowLocation = arrowLocation, styleClass = styleClass) {
    possibleItems.forEach { item ->
        jfxButton(text?.invoke(item), graphic?.invoke(item)) {
            if (itemStyleClass != null) addClass(itemStyleClass)
            addClass(CommonStyle.fillAvailableWidth)
            setOnAction { selectedItemProperty.value = item }
        }
        menuOp(item)
    }
}.apply {
    if (text != null) textProperty().bind(selectedItemProperty.map { text(it!!) })
    if (graphic != null) graphicProperty().bind(selectedItemProperty.map { graphic(it!!) })
}

// TODO: Change style classes to lists.
inline fun <T> EventTarget.popoverComboMenu(
    possibleItems: ObservableList<T>,
    selectedItemProperty: Property<T>,
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    styleClass: CssRule? = null,
    itemStyleClass: CssRule? = null,
    noinline text: ((T) -> String)? = null,
    noinline graphic: ((T) -> Node)? = null,
    crossinline menuOp: VBox.(T) -> Unit = {}
) = buttonWithPopover(arrowLocation = arrowLocation, styleClass = styleClass) {
    possibleItems.perform { items ->
        replaceChildren {
            items.forEach { item ->
                jfxButton(text?.invoke(item), graphic?.invoke(item)) {
                    if (itemStyleClass != null) addClass(itemStyleClass)
                    addClass(CommonStyle.fillAvailableWidth)
                    setOnAction { selectedItemProperty.value = item }
                }
                menuOp(item)
            }
        }
    }
}.apply {
    if (text != null) textProperty().bind(selectedItemProperty.map { if (it != null) text(it) else null })
    if (graphic != null) graphicProperty().bind(selectedItemProperty.map { if (it != null) graphic(it) else null })
}

inline fun popOver(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.(PopOver) -> Unit = {}
): PopOver = PopOver().apply {
    val popover = this
    this.arrowLocation = arrowLocation
    isAnimated = false  // A ton of exceptions start getting thrown if closing a window with an open popover without this.
    isDetachable = false
    isAutoFix = true

    val scrollpane = ScrollPane().apply {
        maxHeight = screenBounds.height * 3 / 4
        isFitToWidth = true
        isFitToHeight = true
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        if (closeOnClick) addEventFilter(MouseEvent.MOUSE_CLICKED) { popover.hide() }
        addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code === KeyCode.ESCAPE) popover.hide() }

        content = VBox().apply {
            addClass(CommonStyle.popoverMenu)
            op(popover)
        }
    }
    contentNode = scrollpane
}

fun PopOver.determineArrowLocation(x: Double, y: Double) = apply {
    val leftBound = screenBounds.maxX / 3
    val rightBound = leftBound * 2
    val topBound = screenBounds.maxY / 3
    val bottomBound = topBound * 2

    val isLeft = x < leftBound
    val isRight = x > rightBound
    val isTop = y < topBound
    val isBottom = y > bottomBound

    arrowLocation = when {
        isLeft -> when {
            isTop -> PopOver.ArrowLocation.TOP_LEFT
            isBottom -> PopOver.ArrowLocation.BOTTOM_LEFT
            else -> PopOver.ArrowLocation.LEFT_CENTER
        }
        isRight -> when {
            isTop -> PopOver.ArrowLocation.TOP_RIGHT
            isBottom -> PopOver.ArrowLocation.BOTTOM_RIGHT
            else -> PopOver.ArrowLocation.RIGHT_CENTER
        }
        else -> when {
            isTop -> PopOver.ArrowLocation.TOP_CENTER
            isBottom -> PopOver.ArrowLocation.BOTTOM_CENTER
            else -> PopOver.ArrowLocation.TOP_LEFT
        }
    }
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

inline fun StackPane.maskerPane(op: MaskerPane.() -> Unit = {}) = opcr(this, MaskerPane(), op)
inline fun StackPane.maskerPane(visible: BooleanProperty, op: MaskerPane.() -> Unit = {}) = maskerPane {
    visibleWhen { visible }
    op()
}

inline fun View.skipFirstTime(op: () -> Unit) {
    val skip = properties.getOrDefault("Gamedex.skipFirstTime", true) as Boolean
    if (skip) {
        properties["Gamedex.skipFirstTime"] = false
    } else {
        op()
    }
}

inline fun Node.popoverContextMenu(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.(PopOver) -> Unit = {}
): PopOver {
    val popover = popOver(arrowLocation, closeOnClick, op).apply { isAutoFix = false; isAutoHide = true }
    addEventHandler(MouseEvent.MOUSE_CLICKED) { popover.hide() }
    setOnContextMenuRequested { e -> popover.show(this@popoverContextMenu, e.screenX, e.screenY) }
    return popover
}

inline fun Node.dropDownMenu(
    arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
    closeOnClick: Boolean = true,
    op: VBox.() -> Unit = {}
): PopOver {
    var popoverHack: PopOver? = null
    val popover = popOver(arrowLocation, closeOnClick) {
        addEventHandler(MouseEvent.MOUSE_EXITED) { popoverHack!!.hide() }
        op(this)
    }
    popoverHack = popover

    addEventHandler(MouseEvent.MOUSE_ENTERED) {
        if (!popover.isShowing) popover.show(this@dropDownMenu)
    }
    setOnMouseExited {
        if (!(it.screenX >= popover.x && it.screenX <= popover.x + popover.width &&
                it.screenY >= popover.y && it.screenY <= popover.y + popover.height)
        ) {
            popover.hide()
        }
    }
    return popover
}

fun ToggleGroup.disallowDeselection() = apply {
    selectedToggleProperty().addListener { _, oldValue, newValue ->
        if (newValue == null) {
            selectToggle(oldValue)
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun EventTarget.imageview(image: ObservableValue<Image>, op: ImageView.() -> Unit = {}) =
    imageview(image as ObservableValue<Image?>, op)

inline fun EventTarget.jfxTabPane(op: JFXTabPane.() -> Unit = {}): JFXTabPane = opcr(this, JFXTabPane(), op)

fun EventTarget.searchField(component: UIComponent, textProperty: StringProperty, op: CustomTextField.() -> Unit = {}) = clearableTextfield(textProperty) {
    promptText = "Search"
    left = Theme.Icon.search(18.0)
    tooltip("Ctrl+f")
    component.shortcut("ctrl+f") { requestFocus() }
    op()
}

inline fun EventTarget.clearableTextfield(textProperty: StringProperty, op: CustomTextField.() -> Unit = {}) = opcr(this, CustomTextField()) {
    useMaxWidth = true
    alignment = Pos.CENTER_LEFT
    textProperty.bindBidirectional(textProperty())

    val clearButton = jfxButton(graphic = Theme.Icon.clear(size = 14.0)) {
        isCancelButton = true
        opacity = 0.0
        cursor = Cursor.DEFAULT
        managedProperty().bind(editableProperty())
        visibleProperty().bind(editableProperty())
        setOnAction {
            requestFocus()
            clear()
        }
    }

    right = StackPane().apply {
        padding {
            top = 4
            bottom = 3
        }
        addChildIfPossible(clearButton)
    }

    val fader = FadeTransition(Duration.millis(350.0), clearButton)
    fader.cycleCount = 1

    val setButtonVisible = { visible: Boolean ->
        fader.fromValue = if (visible) 0.0 else 1.0
        fader.toValue = if (visible) 1.0 else 0.0
        fader.play()
    }

    textProperty().onChange {
        val isTextEmpty = text.isNullOrEmpty()
        val isButtonVisible = fader.node.opacity > 0

        if (isTextEmpty && isButtonVisible) {
            setButtonVisible(false)
        } else if (!isTextEmpty && !isButtonVisible) {
            setButtonVisible(true)
        }
    }

    op()
}

inline fun EventTarget.percentSlider(
    property: Property<Number>,
    min: Number? = null,
    max: Number? = null,
    text: String? = null,
    crossinline op: Slider.() -> Unit = {}
): Slider {
    lateinit var slider: Slider
    hbox(spacing = 5.0, alignment = Pos.CENTER_LEFT) {
        val label = if (text != null) label(text) else null
        slider = slider(min, max) {
            paddingTop = 3.0
            valueProperty().bindBidirectional(property)
            label?.labelFor = this
            op()
        }
        label(slider.valueProperty().asPercent())
    }
    return slider
}

fun ObservableValue<Number>.asPercent() = stringBinding { "${Math.min(((it ?: 0).toDouble() * 100).toInt(), 100)}%" }