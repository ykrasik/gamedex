package com.github.ykrasik.gamedex.common.util

import javafx.application.Platform
import javafx.application.Platform.runLater
import javafx.beans.binding.Binding
import javafx.beans.binding.ObjectBinding
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.Image
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

fun <T, R> ObservableValue<T>.map(f: (T) -> R): Binding<R> = object : ObjectBinding<R>() {
    init {
        super.bind(this)
    }

    override fun computeValue(): R {
        return f(this@map.value)
    }
}

fun <T> ObservableList<T>.unmodifiable(): ObservableList<T> = FXCollections.unmodifiableObservableList(this)