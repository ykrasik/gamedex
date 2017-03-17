package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.common.util.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.common.util.ThreadAwareStringProperty
import com.google.inject.Singleton
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.StringProperty
import tornadofx.getValue
import tornadofx.setValue

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
@Singleton
class NotificationManager {
    private val notification = Notification()
    val messageProperty: ReadOnlyStringProperty get() = notification.messageProperty
    val progressProperty: ReadOnlyDoubleProperty get() = notification.progressProperty

    fun bind(notification: Notification) {
        this.notification.messageProperty.bind(notification.messageProperty)
        this.notification.progressProperty.bind(notification.progressProperty)
    }
}

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