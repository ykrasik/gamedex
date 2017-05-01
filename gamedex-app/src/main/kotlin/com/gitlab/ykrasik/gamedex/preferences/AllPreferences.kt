package com.gitlab.ykrasik.gamedex.preferences

import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.onChange

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
class AllPreferences(
    val general: GeneralPreferences,
    val provider: ProviderPreferences,
    val game: GamePreferences,
    val gameWall: GameWallPreferences
)

abstract class UserPreferencesSet(name: String) {
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
                UserPreferencesSet.update(this@UserPreferencesSet)
            }
        }
        return property
    }

    companion object {
        @JvmStatic
        protected inline fun <reified T : UserPreferencesSet> readOrUse(preferences: T): T {
            val file = preferences.file
            val p = if (file.exists()) {
                file.readJson<T>()
            } else {
                file.create()
                file.writeJson(preferences)
                preferences
            }
            p.updateEnable = true
            return p
        }

        private fun update(p: UserPreferencesSet) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(p.file, p)
        }
    }
}