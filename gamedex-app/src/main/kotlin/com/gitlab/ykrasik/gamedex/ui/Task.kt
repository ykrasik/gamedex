package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.ui.view.MainView
import com.gitlab.ykrasik.gamedex.util.Logger
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.StringProperty
import javafx.geometry.HPos
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.stage.Screen
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.javafx.JavaFx
import tornadofx.*
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
abstract class Task<out T>(private val title: String) {
    protected val log = logger()

    private lateinit var _result: Deferred<T>
    val result: Deferred<T> get() = _result

    private lateinit var _context: CoroutineContext
    val context: CoroutineContext get() = _context

    val progress = Progress(log)

    private var graphic: ImageView by singleAssign()

    val runningProperty = SimpleBooleanProperty(false)

    fun start() {
        _result = async(CommonPool) {
            this@Task._context = context

            run(JavaFx) {
                runningProperty.set(true)
                MainView.showPersistentNotification(GridPane().apply {
                    paddingAll = 10.0
                    hgap = 10.0
                    vgap = 5.0
                    row {
                        label(title) {
                            minWidth = 170.0
                        }
                        progressbar(progress.progressProperty) {
                            prefWidth = Screen.getPrimary().bounds.width
                            gridpaneConstraints { hAlignment = HPos.CENTER }
                        }
                        button(graphic = UIResources.Images.error.toImageView().apply { fitWidth = 30.0; fitHeight = 30.0; isPreserveRatio = true }) {
                            setOnAction { _result.cancel() }
                        }
                    }
                    text(progress.messageProperty) {
                        gridpaneConstraints { columnRowIndex(0, 1); columnSpan = 2; hAlignment = HPos.CENTER }
                    }
                    this@Task.graphic = imageview(UIResources.Images.loading) {
                        fitWidth = 40.0; fitHeight = 40.0; isPreserveRatio = true
                        gridpaneConstraints { columnRowIndex(2, 1); }
                    }
                })
            }

            try {
                doRun()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e)
                }
                throw e
            } finally {
                run(JavaFx) {
                    graphic.image = UIResources.Images.tick
                    runningProperty.set(false)
                    MainView.hidePersistentNotification()
                    MainView.showFlashInfoNotification(doneMessage())
                }
            }
        }
    }

    protected abstract suspend fun doRun(): T
    protected abstract fun doneMessage(): String

    class Progress(private val log: Logger?) {
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
}