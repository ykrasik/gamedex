package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.Filter
import com.gitlab.ykrasik.gamedex.ui.gettingOrElse
import com.gitlab.ykrasik.gamedex.ui.map
import javafx.scene.control.TableColumn
import org.joda.time.Period
import org.joda.time.PeriodType
import tornadofx.getValue
import tornadofx.setValue
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
@Singleton
class GameSettings {
    private val repo = SettingsRepo("game") {
        Data(
            displayType = DisplayType.wall,
            platform = Platform.pc,
            platformSettings = emptyMap(),
            sort = Sort(),
            chooseResults = ChooseResults.chooseIfNonExact,
            stalePeriod = Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime())
        )
    }

    val displayTypeProperty = repo.property(Data::displayType) { copy(displayType = it) }
    var displayType by displayTypeProperty

    // FIXME: Have a per-platform settings thing, like provider user settings
    val platformProperty = repo.property(Data::platform) { copy(platform = it) }
    var platform by platformProperty

    val platformSettingsProperty = repo.property(Data::platformSettings) { copy(platformSettings = it) }
    var platformSettings by platformSettingsProperty

    // TODO: Make this a 2-directional mapping that will auto update.
    val platformSettingsForCurrentPlatformProperty = platformSettingsProperty.gettingOrElse(platformProperty, GamePlatformSettings())
    var platformSettingsForCurrentPlatform by platformSettingsForCurrentPlatformProperty

    val filterForCurrentPlatformProperty = platformSettingsForCurrentPlatformProperty.map { it!!.filter }

    fun setFilter(filter: Filter) {
        platformSettings += (platform to GamePlatformSettings(filter))
    }

    val sortProperty = repo.property(Data::sort) { copy(sort = it) }
    var sort by sortProperty

    val chooseResultsProperty = repo.property(Data::chooseResults) { copy(chooseResults = it) }
    var chooseResults by chooseResultsProperty

    val stalePeriodProperty = repo.property(Data::stalePeriod) { copy(stalePeriod = it) }
    var stalePeriod by stalePeriodProperty

    enum class DisplayType { wall, list }

    data class Sort(
        val sortBy: SortBy = SortBy.criticScore,
        val order: TableColumn.SortType = TableColumn.SortType.DESCENDING
    )

    enum class SortBy(val key: String) {
        name_("Name"),
        criticScore("Critic Score"),
        userScore("User Score"),
        minScore("Min Score"),
        avgScore("Average Score"),
        size("Size"),
        releaseDate("Release Date"),
        updateDate("Update Date");

        override fun toString() = key
    }

    enum class ChooseResults(val key: String) {
        chooseIfNonExact("If no exact match: Choose"),
        alwaysChoose("Always choose"),
        skipIfNonExact("If no exact match: Skip"),
        proceedWithoutIfNonExact("If no exact match: Proceed Without")
    }

    data class Data(
        val displayType: DisplayType = DisplayType.wall,
        val platform: Platform = Platform.pc,
        val platformSettings: Map<Platform, GamePlatformSettings> = emptyMap(),
        val sort: Sort = Sort(),
        val chooseResults: ChooseResults = ChooseResults.chooseIfNonExact,
        val stalePeriod: Period = Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime())
    )
}

data class GamePlatformSettings(
    val filter: Filter = Filter.`true`
)