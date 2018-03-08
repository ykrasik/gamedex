package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.*
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue
import tornadofx.toProperty
import java.util.concurrent.Executors
import kotlin.reflect.KClass

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
class SettingsRepo<T : Any>(name: String, klass: KClass<T>, default: () -> T) {
    private val file = "conf/$name.json".toFile()

    private val initialData: T = run {
        val data = if (file.exists()) {
            try {
                log.trace("[$file] Reading settings file...")
                file.readJson(klass)
            } catch (e: Exception) {
                log.error("[$file] Error reading settings!", e)
                null
            }
        } else {
            log.trace("[$file] Settings file doesn't exist.")
            null
        }
        data ?: default().apply { update(this) }
    }

    private val dataProperty = initialData.toProperty().apply {
        onChange { update(it!!) }
    }
    private var data: T by dataProperty

    fun <R> property(extractor: Extractor<T, R>, modifier: Modifier<T, R>): ObjectProperty<R> =
        dataProperty.mapBidirectional(extractor) { data.modifier(this) }

    fun booleanProperty(extractor: Extractor<T, Boolean>, modifier: Modifier<T, Boolean>): BooleanProperty =
        dataProperty.mapBidirectionalBoolean(extractor) { data.modifier(this) }

    fun stringProperty(extractor: Extractor<T, String>, modifier: Modifier<T, String>): StringProperty =
        dataProperty.mapBidirectionalString(extractor) { data.modifier(this) }

    fun intProperty(extractor: Extractor<T, Int>, modifier: Modifier<T, Int>): IntegerProperty =
        dataProperty.mapBidirectionalInt(extractor) { data.modifier(this) }

    fun doubleProperty(extractor: Extractor<T, Double>, modifier: Modifier<T, Double>): DoubleProperty =
        dataProperty.mapBidirectionalDouble(extractor) { data.modifier(this) }

    private fun update(data: T) = launch(dispatcher) {
        file.create()
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
    }

    fun onChange(f: () -> Unit) = dataProperty.onChange { f() }
    fun perform(f: () -> Unit) = dataProperty.perform { f() }

    companion object {
        private val dispatcher = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "settingsWriter").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        private val log = logger()

        inline operator fun <reified T : Any> invoke(name: String, noinline default: () -> T): SettingsRepo<T> = SettingsRepo(name, T::class, default)
    }
}