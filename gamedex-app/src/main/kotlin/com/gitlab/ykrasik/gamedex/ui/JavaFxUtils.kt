package com.gitlab.ykrasik.gamedex.ui

import javafx.application.Platform
import javafx.application.Platform.runLater
import javafx.beans.binding.ListExpression
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.TransformationList
import javafx.scene.control.TableColumnBase
import javafx.scene.control.TableView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.stage.Stage
import tornadofx.onChange
import java.io.ByteArrayInputStream
import java.util.*

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

fun <T> ObservableList<T>.unmodifiable(): ObservableList<T> = FXCollections.unmodifiableObservableList(this)
fun <T> ObservableList<T>.sizeProperty(): ReadOnlyIntegerProperty {
    val p = SimpleIntegerProperty(this.size)
    this.addListener(ListChangeListener { p.set(this.size)})
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

fun <E, F> ListExpression<E>.mapped(f: (E) -> F): ListProperty<F> = SimpleListProperty(this.value.mapped(f))
fun <E, F> ObservableList<E>.mapped(f: (E) -> F): ObservableList<F> = MappedList(this, f)

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

fun <T, R> Property<T>.map(f: (T?) -> R): Property<R> {
    val property = SimpleObjectProperty(f(this.value))
    this.onChange { property.set(f(it)) }
    return property
}