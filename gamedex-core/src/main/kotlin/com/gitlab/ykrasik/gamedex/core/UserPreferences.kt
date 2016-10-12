package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.provider.jackson.objectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * User: ykrasik
 * Date: 11/10/2016
 * Time: 10:34
 */
class UserPreferences {
    private var update = false

    var autoSkip: Boolean = false
        set(value) {
            field = value
            update()
        }

    var showLog: Boolean = true
        set(value) {
            field = value
            update()
        }

    var logDividerPosition: Double = 0.98
        set(value) {
            field = value
            update()
        }

    var gameWallImageDisplay: GameWallImageDisplay = GameWallImageDisplay.fit
        set(value) {
            field = value
            update()
        }

    var gameSort: GameSort = GameSort.nameAsc
        set(value) {
            field = value
            update()
        }

    var prevDirectory: Path? = null
        set(value) {
            field = value
            update()
        }

    private fun update() {
        if (update) {
            UserPreferencesService.update(this)
        }
    }

    internal fun enableUpdates(): UserPreferences {
        update = true
        return this
    }
}

internal object UserPreferencesService {
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

    fun preferences(): UserPreferences = file.readText().let {
        val p = if (it.isEmpty()) {
            UserPreferences()
        } else {
            objectMapper.readValue(it, UserPreferences::class.java)
        }
        p.enableUpdates()
    }

    fun update(p: UserPreferences) {
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(p)
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