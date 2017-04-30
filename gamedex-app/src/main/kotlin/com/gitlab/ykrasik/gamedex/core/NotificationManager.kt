package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.ui.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.ui.ThreadAwareStringProperty
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
// TODO: This class should be able to display 2 types of notifications - FlashNotifications (which disappear after a second or 2)
// TODO: And PersistentNotifications (which display ongoing job messages & progress).
// TODO: Consider ControlsFx TaskProgressView for this.
@Singleton
class NotificationManager {
    private val notification = Notification()
    val messageProperty: ReadOnlyStringProperty get() = notification.messageProperty
    val progressProperty: ReadOnlyDoubleProperty get() = notification.progressProperty

    fun message(message: String) {
        notification.messageProperty.value = message
    }

    fun progress(done: Int, total: Int) {
        notification.progress(done, total)
    }

    fun <T> with(notification: Notification, f: Notification.() -> T): T {
        bind(notification)
        val retVal = f(notification)
        unbind()
        return retVal
    }

    fun bind(notification: Notification) {
        this.notification.messageProperty.bind(notification.messageProperty)
        this.notification.progressProperty.bind(notification.progressProperty)
    }

    fun unbind() {
        this.notification.messageProperty.unbind()
        this.notification.progressProperty.unbind()
    }
}

// TODO: Remove this class and just send messages to notificationManager.
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