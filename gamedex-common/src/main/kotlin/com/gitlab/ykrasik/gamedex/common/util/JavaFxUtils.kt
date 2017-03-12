package com.gitlab.ykrasik.gamedex.common.util

import javafx.application.Platform
import javafx.application.Platform.runLater
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.TableView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.io.ByteArrayInputStream

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 20:45
 */
fun runLaterIfNecessary(f: () -> Unit) = if (Platform.isFxApplicationThread()) {
    f()
} else {
    runLater(f)
}

fun ByteArray.toImage(): Image = Image(ByteArrayInputStream(this))
fun ByteArray.toImageView(): ImageView = this.toImage().toImageView()
fun Image.toImageView(): ImageView = ImageView(this)

//fun <T, R> ObservableValue<T>.map(f: (T) -> R): Binding<R> = object : ObjectBinding<R>() {
//    init {
//        super.bind(this)
//    }
//
//    override fun computeValue(): R {
//        return f(this@map.value)
//    }
//}

fun <T> ObservableList<T>.unmodifiable(): ObservableList<T> = FXCollections.unmodifiableObservableList(this)
fun <T> ObservableList<T>.sizeProperty(): ReadOnlyIntegerProperty {
    val p = SimpleIntegerProperty(this.size)
    p.addListener { _, _, newValue -> p.set(newValue.toInt()) }
    return p
}

fun <S> TableView<S>.clearSelection() = selectionModel.clearSelection()