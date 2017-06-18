package com.gitlab.ykrasik.gamedex.settings

import com.gitlab.ykrasik.gamedex.core.ReportConfig
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 18/06/2017
 * Time: 11:33
 */
class ReportSettings : SettingsScope() {
    @Transient
    val reportsProperty = preferenceProperty(emptyMap<String, ReportConfig>())
    var reports by reportsProperty
}