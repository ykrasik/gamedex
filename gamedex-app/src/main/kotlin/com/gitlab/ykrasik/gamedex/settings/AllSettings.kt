package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
class AllSettings(
    val general: GeneralSettings,
    val provider: ProviderSettings,
    val game: GameSettings,
    val gameWall: GameWallSettings
)

// TODO: I probably only want 1 class, and the ability to scope settings.
abstract class AbstractSettings(name: String) {
    @Transient
    protected val file = "conf/$name.json".toFile()

    // Jackson constructs the objects by calling it's setters on the properties. Calling a setter = write to file.
    // Disable writing the object to the file while it is being constructed.
    @Transient
    protected var updateEnable = false

    protected fun <T> preferenceProperty(initialValue: T): ObjectProperty<T> {
        val property = SimpleObjectProperty<T>(initialValue)
        property.onChange {
            if (updateEnable) {
                AbstractSettings.update(this@AbstractSettings)
            }
        }
        return property
    }

    companion object {
        @JvmStatic
        protected inline fun <reified T : AbstractSettings> readOrUse(settings: T): T {
            val file = settings.file
            val p = if (file.exists()) {
                file.readJson<T>()
            } else {
                file.create()
                file.writeJson(settings)
                settings
            }
            p.updateEnable = true
            return p
        }

        private fun update(s: AbstractSettings) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(s.file, s)
        }
    }
}