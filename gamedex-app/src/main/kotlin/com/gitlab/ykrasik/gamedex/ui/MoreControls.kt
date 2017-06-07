package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.ui.theme.CommonStyle
import com.gitlab.ykrasik.gamedex.ui.widgets.FixedRatingSkin
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewResizingPane
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXToggleButton
import com.jfoenix.controls.JFXToggleNode
import javafx.beans.property.Property
import javafx.beans.value.ChangeListener
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import javafx.util.Callback
import org.controlsfx.control.PopOver
import org.controlsfx.control.Rating
import tornadofx.*
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 09:33
 */
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

fun EventTarget.fixedRating(max: Int, isPartial: Boolean = true, op: (Rating.() -> Unit)? = null) = opcr(this, Rating(max), op).apply {
    isPartialRating = isPartial
    skin = FixedRatingSkin(this)
}

fun EventTarget.imageViewResizingPane(imageView: ImageView, op: (ImageViewResizingPane.() -> Unit)? = null) =
    opcr(this, ImageViewResizingPane(imageView), op)

fun Node.clipRectangle(op: Rectangle.() -> Unit) {
    clip = Rectangle().apply(op)
}

fun EventTarget.jfxToggleButton(op: (JFXToggleButton.() -> Unit)? = null) = opcr(this, JFXToggleButton(), op)
fun Node.jfxToggleNode(graphic: Node? = null, group: ToggleGroup? = getToggleGroup(), op: (JFXToggleNode.() -> Unit)? = null) = opcr(this, JFXToggleNode().apply {
    this.graphic = graphic
    this.toggleGroup = group
}, op)

fun EventTarget.jfxButton(text: String? = null, graphic: Node? = null, type: JFXButton.ButtonType = JFXButton.ButtonType.FLAT, op: (JFXButton.() -> Unit)? = null) =
    opcr(this, JFXButton().apply {
        addClass(CommonStyle.jfxButton)
        this.text = text
        this.graphic = graphic
        this.buttonType = type
    }, op)

fun EventTarget.buttonWithPopover(text: String? = null,
                                  graphic: Node? = null,
                                  styleClass: CssRule? = CommonStyle.toolbarButton,
                                  arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                  closeOnClick: Boolean = true,
                                  op: (VBox.() -> Unit)? = null) =
    jfxButton(text = text, graphic = graphic) {
        if (styleClass != null) addClass(styleClass)
        val popover = popOver(arrowLocation, closeOnClick, op)
        setOnAction { popover.toggle(this) }
    }

// TODO: This is a bit too much.
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

fun <T> EventTarget.popoverToggleMenu(possibleItems: ObservableList<T>,
                                      selectedItems: Property<List<T>>,
                                      arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                      styleClasses: List<CssRule> = emptyList(),
                                      itemStyleClasses: List<CssRule> = emptyList(),
                                      text: ((T) -> String)? = null,
                                      graphic: ((T) -> Node)? = null,
                                      menuOp: (VBox.(T) -> Unit)? = null) =
    buttonWithPopover(arrowLocation = arrowLocation, styleClass = null, closeOnClick = false) {
        val selectedItemsListeners = mutableListOf<ChangeListener<List<T>>>()
        possibleItems.performing { items ->
            selectedItemsListeners.forEach { selectedItems.removeListener(it) }
            selectedItemsListeners.clear()

            // TODO: Review all places where this is used for listener leaks.
            replaceChildren {
                items.forEach { item ->
                    jfxToggleNode {
                        addClass(CommonStyle.toggleMenuButton)
                        this.graphic = label {
                            addClass(CommonStyle.toggleMenuContent, CommonStyle.fillAvailableWidth)
                            itemStyleClasses.forEach { addClass(it) }
                            this.text = text?.invoke(item)
                            this.graphic = graphic?.invoke(item)
                        }
                        isSelected = selectedItems.value.contains(item)

                        selectedProperty().onChange {
                            selectedItems.value = if (it) (selectedItems.value + item).distinct() else selectedItems.value - item
                        }

                        selectedItemsListeners += selectedItems.changeListener {
                            isSelected = selectedItems.value.contains(item)
                        }
                    }
                    menuOp?.invoke(this@buttonWithPopover, item)
                }
            }
        }
    }.apply {
        styleClasses.forEach { addClass(it) }
        if (text != null) {
            textProperty().bind(selectedItems.map { selectedItems ->
                if (selectedItems!!.isEmpty() || selectedItems.toSet() == possibleItems.toSet()) "All"
                else selectedItems.map(text).sorted().joinToString(", ")
            })
        }
    }

fun popOver(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
            closeOnClick: Boolean = true,
            op: (VBox.() -> Unit)? = null): PopOver = PopOver().apply {
    this.arrowLocation = arrowLocation
    isAnimated = false  // A ton of exceptions start getting thrown if closing a window with an open popover without this.
    isDetachable = false

    val scrollpane = ScrollPane().apply {
        maxHeight = Screen.getPrimary().bounds.height * 3 / 4
        isFitToWidth = true
        isFitToHeight = true
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
    }
    contentNode = scrollpane
    val content = VBox().apply {
        addClass(CommonStyle.popoverMenu)
        scrollpane.content = this
    }
    if (closeOnClick) content.addEventFilter(MouseEvent.MOUSE_CLICKED) { hide() }
    content.addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code === KeyCode.ESCAPE) hide() }
    op?.invoke(content)
}

fun PopOver.toggle(parent: Node) = if (isShowing) hide() else show(parent)

//fun Node.dropDownMenu(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT, op: (PopOver.() -> Unit)? = null): PopOver {
//    val popover = popOver(arrowLocation)
//    this@dropDownMenu.setOnMouseEntered {
//        if (!popover.isShowing) {
//            popover.show(this@dropDownMenu)
//        }
//    }
//    this@dropDownMenu.setOnMouseExited {
//        if (!(it.screenX >= popover.x && it.screenX <= popover.x + popover.width &&
//            it.screenY >= popover.y && it.screenY <= popover.y + popover.height)) {
//            popover.hide()
//        }
//    }
//    op?.invoke(popover)
//    return popover
//}