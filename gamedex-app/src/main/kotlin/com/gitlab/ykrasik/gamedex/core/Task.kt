package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.widgets.Notification
import com.gitlab.ykrasik.gamedex.util.Logger
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.StringProperty
import javafx.geometry.Pos
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.*
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
// TODO: Consider having a notificationPane and only 1 possible ongoing task at a time.
abstract class GamedexTask<T>(title: String) {
    private val log = logger()

    private lateinit var _result: Deferred<T>
    val result: Deferred<T> get() = _result

    protected val progress = TaskProgress(log)

    private var graphic: ImageView by singleAssign()

    // TODO: Make the notification moveable
    private val notification = Notification()
        .hideCloseButton()
        .position(Pos.TOP_RIGHT)
        .title(title)
        .graphic(GridPane().apply {
            hgap = 10.0
            vgap = 5.0
            progressbar(progress.progressProperty) {
                gridpaneConstraints { columnIndex = 0; rowIndex = 0 }
                minWidth = 500.0
            }
            button(graphic = UIResources.Images.error.toImageView().apply { fitWidth = 30.0; fitHeight = 30.0; isPreserveRatio = true }) {
                gridpaneConstraints { columnIndex = 2; rowIndex = 0 }
                setOnAction { _result.cancel() }
            }
            text(progress.messageProperty) { gridpaneConstraints { columnIndex = 0; rowIndex = 1 } }
            region { gridpaneConstraints { columnIndex = 1; rowIndex = 1; hGrow = Priority.ALWAYS } }
            graphic = imageview(UIResources.Images.loading) {
                gridpaneConstraints { columnIndex = 2; rowIndex = 1 }
                fitWidth = 40.0; fitHeight = 40.0; isPreserveRatio = true
            }
        })

    val runningProperty = SimpleBooleanProperty(false)

    fun start() {
        _result = async(CommonPool) {
            run(JavaFx) {
                runningProperty.set(true)
                notification.show()
            }

            try {
                doRun(context)
            } finally {
                run(JavaFx) {
                    finally()
                    graphic.image = UIResources.Images.tick
                    notification.hide(afterDelay = 1.seconds)
                    runningProperty.set(false)
                }
            }
        }
    }

    protected abstract suspend fun doRun(context: CoroutineContext): T
    protected abstract fun finally()
}

class TaskProgress(private val log: Logger?) {
    val messageProperty: StringProperty = ThreadAwareStringProperty()
    val progressProperty: DoubleProperty = ThreadAwareDoubleProperty()

    var message: String
        get() = messageProperty.get()
        set(value) {
            messageProperty.set(value)
            log?.info(value)
        }

    var progress: Double by progressProperty

    fun progress(done: Int, total: Int) {
        progress = done.toDouble() / total.toDouble()
    }
}