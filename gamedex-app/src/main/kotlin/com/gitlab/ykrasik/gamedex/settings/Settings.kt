package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
abstract class Settings(name: String) {
    @Transient
    protected val file = "conf/$name.json".toFile()

    // Jackson constructs the objects by calling it's setters on the properties. Calling a setter = write to file.
    // Disable writing the object to the file while it is being constructed.
    @Transient
    protected var updateEnabled = false

    @Transient
    val changedProperty = SimpleBooleanProperty().apply {
        onChange {
            if (it && updateEnabled) update()
        }
    }

    protected fun <T> preferenceProperty(initialValue: T): ObjectProperty<T> = SimpleObjectProperty(initialValue).notifyOnChange()
    protected fun preferenceProperty(initialValue: Boolean): BooleanProperty = SimpleBooleanProperty(initialValue).notifyOnChange()
    protected fun preferenceProperty(initialValue: Int): IntegerProperty = SimpleIntegerProperty(initialValue).notifyOnChange()
    protected fun preferenceProperty(initialValue: Double): DoubleProperty = SimpleDoubleProperty(initialValue).notifyOnChange()
    protected fun preferenceProperty(initialValue: String): StringProperty = SimpleStringProperty(initialValue).notifyOnChange()

    private fun <T : Property<R>, R> T.notifyOnChange() = apply {
        onChange {
            // Fire a change event and reset the value.
            changedProperty.value = true
            changedProperty.value = false
        }
    }

    private fun update() = launch(CommonPool) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, this@Settings)
    }

    companion object {
        @JvmStatic
        protected inline fun <reified T : Settings> readOrUse(settings: T): T {
            val file = settings.file
            val p = if (file.exists()) {
                file.readJson<T>()
            } else {
                file.create()
                file.writeJson(settings)
                settings
            }
            p.updateEnabled = true
            return p
        }
    }
}