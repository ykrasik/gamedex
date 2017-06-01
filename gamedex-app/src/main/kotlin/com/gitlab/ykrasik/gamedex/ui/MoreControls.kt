package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ui.widgets.FixedRatingSkin
import com.gitlab.ykrasik.gamedex.ui.widgets.ImageViewResizingPane
import com.jfoenix.controls.*
import javafx.beans.property.Property
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.util.Callback
import org.controlsfx.control.PopOver
import org.controlsfx.control.Rating
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import kotlin.reflect.KProperty1

/**
 * User: ykrasik
 * Date: 16/05/2017
 * Time: 09:33
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

fun EventTarget.fixedRating(max: Int, isPartial: Boolean = true, op: (Rating.() -> Unit)? = null) = opcr(this, Rating(max), op).apply {
    isPartialRating = isPartial
    skin = FixedRatingSkin(this)
}

fun EventTarget.imageViewResizingPane(imageView: ImageView, op: (ImageViewResizingPane.() -> Unit)? = null) =
    opcr(this, ImageViewResizingPane(imageView), op)

fun Node.clipRectangle(op: Rectangle.() -> Unit) {
    clip = Rectangle().apply(op)
}

fun EventTarget.jfxHamburger(op: (JFXHamburger.() -> Unit)? = null) = opcr(this, JFXHamburger(), op)
fun EventTarget.jfxDrawer(op: (JFXDrawer.() -> Unit)? = null) = opcr(this, JFXDrawer(), op)
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

fun EventTarget.jfxButton(text: Property<String>, graphic: Node? = null, type: JFXButton.ButtonType = JFXButton.ButtonType.FLAT, op: (JFXButton.() -> Unit)? = null) =
    jfxButton(text.value, graphic, type, op).apply {
        textProperty().cleanBind(text)
    }

fun EventTarget.buttonWithPopover(text: String? = null,
                                  graphic: Node? = null,
                                  arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                  styleClass: CssRule? = CommonStyle.toolbarButton,
                                  op: (PopOver.() -> Unit)? = null) =
    jfxButton(text = text, graphic = graphic) {
        styleClass?.let { addClass(it) }
        withPopover(arrowLocation) {
            op?.invoke(this)
        }
    }

// TODO: This is insane, really.
fun <T> EventTarget.popoverComboMenu(possibleItems: ObservableList<T>,
                                     selectedItemProperty: Property<T>,
                                     arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                     styleClass: CssRule? = CommonStyle.toolbarButton,
                                     itemStyleClass: CssRule? = null,
                                     text: ((T) -> String)? = null,
                                     graphic: ((T) -> Node)? = null,
                                     menuItemOp: (PopOver.(T) -> Unit)? = null) {
    buttonWithPopover(arrowLocation = arrowLocation, styleClass = styleClass) {
        possibleItems.performing { items ->
            popoverContent.replaceChildren {
                items.forEach { item ->
                    popoverMenuItem(text?.invoke(item), graphic?.invoke(item), styleClass = itemStyleClass) {
                        selectedItemProperty.value = item
                    }
                    if (item == selectedItemProperty) {
                        selectedItemProperty.value = item
                    }
                    menuItemOp?.invoke(this@buttonWithPopover, item)
                }
            }
        }
    }.apply {
        if (text != null) textProperty().bind(selectedItemProperty.map { text(it!!) })
        if (graphic != null) graphicProperty().bind(selectedItemProperty.map { graphic(it!!) })
    }
}

fun <T> EventTarget.popoverToggleMenu(possibleItems: ObservableList<T>,
                                      selectedItems: Property<List<T>>,
                                      arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT,
                                      styleClass: CssRule? = CommonStyle.toolbarButton,
                                      itemStyleClass: CssRule? = null,
                                      text: ((T) -> String)? = null,
                                      graphic: ((T) -> Node)? = null,
                                      menuItemOp: (PopOver.(T) -> Unit)? = null) {
    buttonWithPopover(arrowLocation = arrowLocation, styleClass = styleClass) {
        possibleItems.performing { items ->
            popoverContent.replaceChildren {
                items.forEach { item ->
                    hbox {
                        jfxToggleButton {
                            if (itemStyleClass != null) addClass(itemStyleClass)
                            this.text = text?.invoke(item)
                            isSelected = selectedItems.value.contains(item)
                            selectedProperty().onChange {
                                if (it) selectedItems.value += item else selectedItems.value -= item
                            }
                        }
                        if (graphic != null) {
                            children += graphic(item).apply {
                                alignment = Pos.CENTER
                                paddingRight = 5.0
                            }
                        }
                    }
                    menuItemOp?.invoke(this@buttonWithPopover, item)
                }
            }
        }
    }.apply {
        if (text != null) {
            textProperty().bind(selectedItems.map { selectedItems ->
                if (selectedItems!!.isEmpty() || selectedItems == possibleItems) "All" else selectedItems.map(text).sorted().joinToString(", ")
            })
        }
    }
}

fun PopOver.popoverMenuItem(text: String? = null,
                            graphic: Node? = null,
                            styleClass: CssRule? = CommonStyle.extraMenu,
                            onAction: () -> Unit): JFXButton {
    return popoverContent.jfxButton(text, graphic) {
        if (styleClass != null) addClass(styleClass)
        setOnAction {
            this@popoverMenuItem.hide()
            onAction()
        }
    }
}

fun PopOver.separator(orientation: Orientation = Orientation.HORIZONTAL, op: (Separator.() -> Unit)? = null) {
    popoverContent.separator(orientation, op)
}

val PopOver.popoverContent: VBox get() {
    return if (contentNode !is VBox) {
        VBox().apply {
            addClass(CommonStyle.popoverMenu)
            contentNode = this
        }
    } else {
        contentNode as VBox
    }
}

fun <T> EventTarget.jfxComboBox(property: Property<T>? = null, values: List<T>? = null, op: (JFXComboBox<T>.() -> Unit)? = null) = opcr(this, JFXComboBox<T>().apply {
    if (values != null) items = if (values is ObservableList<*>) values as ObservableList<T> else values.observable()
    if (property != null) valueProperty().bindBidirectional(property)
}, op)

fun popOver(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT, op: (PopOver.() -> Unit)? = null): PopOver =
    PopOver().apply {
        this.arrowLocation = arrowLocation
        isAnimated = false  // A ton of exceptions start getting thrown if closing a window with an open popover without this.
        isDetachable = false
        op?.invoke(this)
    }

fun Button.withPopover(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT, op: (PopOver.() -> Unit)? = null) {
    val popover = popOver(arrowLocation, op)
    setOnAction {
        if (popover.isShowing) popover.hide() else popover.show(this)
    }
}

fun Platform.toLogo(op: (Glyph.() -> Unit)? = null) = when (this) {
    Platform.pc -> FontAwesome.Glyph.WINDOWS.toGraphic { color(Color.CORNFLOWERBLUE); size(19.0) }
    Platform.android -> FontAwesome.Glyph.ANDROID.toGraphic { color(Color.FORESTGREEN); size(19.0) }
    Platform.mac -> FontAwesome.Glyph.APPLE.toGraphic { color(Color.GRAY); size(19.0) }
    else -> FontAwesome.Glyph.QUESTION.toGraphic { size(19.0) }
}.apply { op?.invoke(this) }

fun Node.dropDownMenu(arrowLocation: PopOver.ArrowLocation = PopOver.ArrowLocation.TOP_LEFT, op: (PopOver.() -> Unit)? = null): PopOver {
    val popover = popOver(arrowLocation)
    this@dropDownMenu.setOnMouseEntered {
        if (!popover.isShowing) {
            popover.show(this@dropDownMenu)
        }
    }
    this@dropDownMenu.setOnMouseExited {
        if (!(it.screenX >= popover.x && it.screenX <= popover.x + popover.width &&
            it.screenY >= popover.y && it.screenY <= popover.y + popover.height)) {
            popover.hide()
        }
    }
    op?.invoke(popover)
    return popover
}