package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.core.ReportRule
import com.gitlab.ykrasik.gamedex.ui.gettingOrElse
import javafx.scene.control.TableColumn
import org.joda.time.Period
import org.joda.time.PeriodType
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 01/05/2017
 * Time: 19:08
 */
class GameSettings private constructor() : Settings("game") {
    companion object {
        operator fun invoke(): GameSettings = readOrUse(GameSettings())
    }

    @Transient
    val displayTypeProperty = preferenceProperty(DisplayType.wall)
    var displayType by displayTypeProperty

    @Transient
    val platformProperty = preferenceProperty(Platform.pc)
    var platform by platformProperty

    @Transient
    val filterProperty = preferenceProperty(emptyMap<Platform, ReportRule>())
    var filter by filterProperty

    @Transient
    val filterForPlatformProperty = filterProperty.gettingOrElse(platformProperty, ReportRule.Rules.True())

    @Transient
    val sortProperty = preferenceProperty(Sort())
    var sort by sortProperty

    @Transient
    val chooseResultsProperty = preferenceProperty(ChooseResults.chooseIfNonExact)
    var chooseResults by chooseResultsProperty

    @Transient
    val stalePeriodProperty = preferenceProperty(Period.months(2).normalizedStandard(PeriodType.yearMonthDayTime()))
    var stalePeriod by stalePeriodProperty

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

    enum class DisplayType { wall, list }

    enum class ChooseResults(val key: String) {
        chooseIfNonExact("If no exact match: Choose"),
        alwaysChoose("Always choose"),
        skipIfNonExact("If no exact match: Skip"),
        proceedWithoutIfNonExact("If no exact match: Proceed Without")
    }
}

fun GameSettings.setFilter(rule: ReportRule) {
    filter += (platform to rule)
}

fun GameSettings.modifyFilter(f: (ReportRule) -> ReportRule) {
    setFilter(f(filterForPlatformProperty.value))
}