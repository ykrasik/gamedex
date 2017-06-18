package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.*
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
// TODO: This could create an issue for testing.
class Settings private constructor(
    val general: GeneralSettings = GeneralSettings(),
    val provider: ProviderSettings = ProviderSettings(),
    val game: GameSettings = GameSettings(),
    val gameWall: GameWallSettings = GameWallSettings(),
    val report: ReportSettings = ReportSettings()
) {
    // Jackson constructs the objects by calling it's setters on the properties. Calling a setter = write to file.
    // Disable writing the object to the file while it is being constructed.
    @Transient
    private var updateEnabled = false

    init {
        general.updateOnChange()
        provider.updateOnChange()
        game.updateOnChange()
        gameWall.updateOnChange()
        report.updateOnChange()
    }

    private fun SettingsScope.updateOnChange() = changedProperty.onChange {
        if (it && updateEnabled) update()
    }

    private fun update() = objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, this)

    companion object {
        private val file = "settings.json".toFile()

        operator fun invoke(): Settings {
            val settings = if (file.exists()) {
                file.readJson<Settings>()
            } else {
                file.create()
                Settings().apply { file.writeJson(this) }
            }
            settings.updateEnabled = true
            return settings
        }
    }
}

abstract class SettingsScope {
    @Transient
    val changedProperty = SimpleBooleanProperty()

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

    abstract inner class SubSettings : SettingsScope() {
        init {
            this.changedProperty.onChange { this@SettingsScope.changedProperty.value = it }
        }
    }
}