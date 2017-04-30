package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.ui.ThreadAwareDoubleProperty
import com.gitlab.ykrasik.gamedex.ui.ThreadAwareStringProperty
import com.gitlab.ykrasik.gamedex.ui.UIResources
import com.gitlab.ykrasik.gamedex.ui.toImageView
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.StringProperty
import javafx.geometry.Pos
import javafx.scene.layout.GridPane
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import tornadofx.*
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
// TODO: Support 2 types of notifications - FlashNotifications (which disappear after a second or 2)
// TODO: And PersistentNotifications (which display ongoing job messages & progress).
abstract class GamedexTask(title: String) {
    protected val log by logger()

    private lateinit var job: Job
    protected val progress = TaskProgress()

    private val notification = Notification()
        .hideCloseButton()
        .position(Pos.TOP_RIGHT)
        .title(title)
        .graphic(GridPane().apply {
            hgap = 10.0
            vgap = 5.0
            row {
                progressbar(progress.progressProperty) {
                    minWidth = 500.0
                }
                button(graphic = UIResources.Images.error.toImageView()) {
                    setOnAction { job.cancel() }
                }
            }
            row {
                text(progress.messageProperty)
            }
        })

    val runningProperty = SimpleBooleanProperty(false)

    fun start() {
        job = launch(CommonPool) {
            run(JavaFx) {
                runningProperty.set(true)
                notification.show()
            }

            try {
                run(CommonPool) {
                    doRun(context)
                }
            } finally {
                run(JavaFx) {
                    finally()
                    notification.hide(afterDelay = 2.seconds)
                    runningProperty.set(false)
                }
            }
        }
    }

    protected abstract suspend fun doRun(context: CoroutineContext)
    protected abstract fun finally()
}

class TaskProgress(private val writeToLog: Boolean = true) {
    val messageProperty: StringProperty = ThreadAwareStringProperty()
    val progressProperty: DoubleProperty = ThreadAwareDoubleProperty()

    var message: String
        get() = messageProperty.get()
        set(value) {
            messageProperty.set(value)
            if (writeToLog) {
                log.info(value)
            }
        }

    var progress: Double by progressProperty

    fun progress(done: Int, total: Int) {
        progress = done.toDouble() / total.toDouble()
    }

    companion object {
        private val log by logger()
    }
}