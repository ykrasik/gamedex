package com.gitlab.ykrasik.gamedex.preferences

import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.util.*
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.getValue
import tornadofx.onChange
import tornadofx.setValue
import java.io.File

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
class UserPreferences private constructor() {
    // Jackson constructs the objects by calling it's setters on the properties. Calling a setter = write to file.
    // Disable writing the object to the file while it is being constructed.
    @Transient
    private var updateEnable = false

    @Transient
    val handsFreeModeProperty = preferenceProperty(false)
    var handsFreeMode by handsFreeModeProperty

    @Transient
    val showLogProperty = preferenceProperty(true)
    var showLog by showLogProperty

    @Transient
    val logDividerPositionProperty = preferenceProperty(0.98)
    var logDividerPosition by logDividerPositionProperty

    @Transient
    val gameWallImageDisplayTypeProperty = preferenceProperty(ImageDisplayType.stretch)
    var gameWallImageDisplayType by gameWallImageDisplayTypeProperty

    @Transient
    val gameSortProperty = preferenceProperty(GameSort.criticScoreDesc)
    var gameSort by gameSortProperty

    @Transient
    val prevDirectoryProperty: ObjectProperty<File?> = preferenceProperty(null)
    var prevDirectory by prevDirectoryProperty

    @Transient
    val providerNamePriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.GiantBomb))
    var providerNamePriority by providerNamePriorityProperty

    @Transient
    val providerDescriptionPriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.Igdb))
    var providerDescriptionPriority by providerDescriptionPriorityProperty

    @Transient
    val providerReleaseDatePriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.GiantBomb))
    var providerReleaseDatePriority by providerReleaseDatePriorityProperty

    @Transient
    val providerCriticScorePriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.Igdb))
    var providerCriticScorePriority by providerCriticScorePriorityProperty

    @Transient
    val providerUserScorePriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.Igdb))
    var providerUserScorePriority by providerUserScorePriorityProperty

    @Transient
    val providerThumbnailPriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.GiantBomb))
    var providerThumbnailPriority by providerThumbnailPriorityProperty

    @Transient
    val providerPosterPriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.GiantBomb))
    var providerPosterPriority by providerPosterPriorityProperty

    @Transient
    val providerScreenshotPriorityProperty = preferenceProperty(DefaultProviderPriority.prefer(GameProviderType.Igdb))
    var providerScreenshotPriority by providerScreenshotPriorityProperty

    @Transient
    val logFilterLevelProperty = preferenceProperty(LogLevel.info)
    var logFilterLevel by logFilterLevelProperty

    @Transient
    val logTailProperty = preferenceProperty(true)
    var logTail by logTailProperty

    companion object {
        private val file = "conf/conf.json".toFile()

        operator fun invoke(): UserPreferences {
            val p = if (file.exists()) {
                file.readJson<UserPreferences>()
            } else {
                file.create()
                UserPreferences().apply {
                    file.writeJson(this)
                }
            }
            p.updateEnable = true
            return p
        }

        private fun update(p: UserPreferences) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, p)
        }
    }

    private fun <T> preferenceProperty(initialValue: T): ObjectProperty<T> {
        val property = SimpleObjectProperty<T>(initialValue)
        property.onChange {
            if (updateEnable) {
                UserPreferences.update(this@UserPreferences)
            }
        }
        return property
    }
}