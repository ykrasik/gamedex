package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.ui.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.ui.ThreadAwareStringProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.StringProperty
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
// TODO: Support 2 types of notifications - FlashNotifications (which disappear after a second or 2)
// TODO: And PersistentNotifications (which display ongoing job messages & progress).

// TODO: Rename this to TaskInfo?
data class Notification(
    val messageProperty: StringProperty = ThreadAwareStringProperty(),
    val progressProperty: DoubleProperty = ThreadAwareDoubleProperty()
) {
    var message: String by messageProperty
    var progress: Double by progressProperty

    fun progress(done: Int, total: Int) {
        progress = done.toDouble() / total.toDouble()
    }
}