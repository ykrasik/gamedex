package com.gitlab.ykrasik.gamedex.core

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
    val autoSkipProperty: ObjectProperty<Boolean> = UserPreferencesProperty(false)
    var autoSkip by autoSkipProperty

    @Transient
    val showLogProperty: ObjectProperty<Boolean> = UserPreferencesProperty(true)
    var showLog by showLogProperty

    @Transient
    val logDividerPositionProperty: ObjectProperty<Double> = UserPreferencesProperty(0.98)
    var logDividerPosition by logDividerPositionProperty

    @Transient
    val gameWallImageDisplayTypeProperty: ObjectProperty<ImageDisplayType> = UserPreferencesProperty(ImageDisplayType.stretch)
    var gameWallImageDisplayType by gameWallImageDisplayTypeProperty

    @Transient
    val gameSortProperty: ObjectProperty<GameSort> = UserPreferencesProperty(GameSort.nameAsc)
    var gameSort by gameSortProperty

    @Transient
    val prevDirectoryProperty: ObjectProperty<File?> = UserPreferencesProperty(null)
    var prevDirectory by prevDirectoryProperty

    @Transient
    val handsFreeModeProperty: ObjectProperty<Boolean> = UserPreferencesProperty(false)
    var handsFreeMode by handsFreeModeProperty

    @Transient
    val providerNamePriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.GiantBomb))
    val providerNamePriority by providerNamePriorityProperty

    @Transient
    val providerDescriptionPriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.Igdb))
    val providerDescriptionPriority by providerDescriptionPriorityProperty

    @Transient
    val providerReleaseDatePriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.GiantBomb))
    val providerReleaseDatePriority by providerReleaseDatePriorityProperty

    @Transient
    val providerCriticScorePriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.Igdb))
    val providerCriticScorePriority by providerCriticScorePriorityProperty

    @Transient
    val providerUserScorePriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.Igdb))
    val providerUserScorePriority by providerUserScorePriorityProperty

    @Transient
    val providerThumbnailPriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.GiantBomb))
    val providerThumbnailPriority by providerThumbnailPriorityProperty

    @Transient
    val providerPosterPriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.GiantBomb))
    val providerPosterPriority by providerPosterPriorityProperty

    @Transient
    val providerScreenshotPriorityProperty: ObjectProperty<ProviderPriority> = UserPreferencesProperty(preferProvider(GameProviderType.Igdb))
    val providerScreenshotPriority by providerScreenshotPriorityProperty

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

    private inner class UserPreferencesProperty<T>(initialValue: T) : SimpleObjectProperty<T>(initialValue) {
        init {
            this.onChange {
                if (updateEnable) {
                    com.gitlab.ykrasik.gamedex.core.UserPreferences.update(this@UserPreferences)
                }
            }
        }
    }
}

// TODO: Sort this into a class
typealias ProviderPriority = Map<GameProviderType, Int>
fun preferProvider(type: GameProviderType): ProviderPriority =
    GameProviderType.values().associate { it to it.ordinal } + (type to (GameProviderType.values().size + 1))

fun preferredProviderFrom(priority: ProviderPriority): GameProviderType = priority.maxBy { it.value }!!.key

enum class ImageDisplayType {
    fit,
    stretch
//    enlarge
}

// TODO: I don't like displayName on the enum.
enum class GameSort constructor(val displayName: String) {
    nameAsc("Name \u2191"),
    nameDesc("Name \u2193"),
    criticScoreAsc("Critic Score \u2191"),
    criticScoreDesc("Critic Score \u2193"),
    userScoreAsc("User Score \u2191"),
    userScoreDesc("User Score \u2193"),
    minScoreAsc("Min Score \u2191"),
    minScoreDesc("Min Score \u2193"),
    avgScoreAsc("Average Score \u2191"),
    avgScoreDesc("Average Score \u2193"),
    releaseDateAsc("Release Date \u2191"),
    releaseDateDesc("Release Date \u2193"),
    dateAddedAsc("Date Added \u2191"),
    dateAddedDesc("Date Added \u2193");

    override fun toString() = displayName
}