package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.jackson.objectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
data class UserPreferences(
    val autoSkip: Boolean = false,
    val showLog: Boolean = true,
    val logDividerPosition: Double = 0.98,
    val gameWallImageDisplay: GameWallImageDisplay = GameWallImageDisplay.fit,
    val gameSort: GameSort = GameSort.nameAsc,
    val pevDirectory: Path? = null
)

data class UserPreferencesBuilder(
    var autoSkip: Boolean = false,
    var showLog: Boolean = true,
    var logDividerPosition: Double = 0.98,
    var gameWallImageDisplay: GameWallImageDisplay = GameWallImageDisplay.fit,
    var gameSort: GameSort = GameSort.nameAsc,
    var pevDirectory: Path? = null
) {
    fun build() = UserPreferences(
        autoSkip = autoSkip,
        showLog = showLog,
        logDividerPosition = logDividerPosition,
        gameWallImageDisplay = gameWallImageDisplay,
        gameSort = gameSort,
        pevDirectory = pevDirectory
    )

    companion object {
        operator fun invoke(p: UserPreferences) = UserPreferencesBuilder(
            autoSkip = p.autoSkip,
            showLog = p.showLog,
            logDividerPosition = p.logDividerPosition,
            gameWallImageDisplay = p.gameWallImageDisplay,
            gameSort = p.gameSort,
            pevDirectory = p.pevDirectory
        )
    }
}

interface UserPreferencesService {
    val preferences: UserPreferences

    fun update(updater: UserPreferencesBuilder.() -> Unit)
}

@Singleton
class UserPreferencesServiceImpl : UserPreferencesService {
    private val fileName = "conf/user.json"
    private val file = Paths.get(fileName).let {
        val path = if (Files.exists(it)) {
            it
        } else {
            Files.createDirectories(it.parent)
            Files.createFile(it)
        }
        path.toFile()
    }

    override var preferences = file.readText().let {
        if (it.isEmpty()) {
            UserPreferences()
        } else {
            objectMapper.readValue(it, UserPreferences::class.java)
        }
    }

    override fun update(updater: UserPreferencesBuilder.() -> Unit) {
        val newPreferences = UserPreferencesBuilder(preferences)
        updater(newPreferences)
        preferences = newPreferences.build()
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(preferences)
        file.writeText(json)
    }
}

enum class ImageDisplayType {
    fit,
    stretch
//    enlarge
}

// TODO: Why 2 enums?
enum class GameWallImageDisplay constructor(val imageDisplayType: ImageDisplayType, val displayName: String) {
    //    enlarge(ImageDisplayType.ENLARGE, "Enlarge Image");
    fit(ImageDisplayType.fit, "Fit Image"),
    stretch(ImageDisplayType.stretch, "Stretch Image");

    override fun toString() = displayName
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