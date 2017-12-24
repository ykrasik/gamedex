package com.gitlab.ykrasik.gamedex.ui

import com.gitlab.ykrasik.gamedex.Platform
import com.gitlab.ykrasik.gamedex.ui.theme.Theme.Images
import com.gitlab.ykrasik.gamedex.ui.theme.toLogo
import com.gitlab.ykrasik.gamedex.ui.view.main.MainView
import com.gitlab.ykrasik.gamedex.util.Logger
import com.gitlab.ykrasik.gamedex.util.logger
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.StringProperty
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.run
import tornadofx.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 16/03/2017
 * Time: 18:04
 */
abstract class Task<out T>(val titleProperty: ThreadAwareStringProperty) {
    constructor(title: String) : this(ThreadAwareStringProperty(title))
    
    protected val log = logger()

    private lateinit var _result: Deferred<T>
    val result: Deferred<T> get() = _result

    private lateinit var _context: CoroutineContext
    val context: CoroutineContext get() = _context

    val isActive get() = result.isActive

    val progress = Progress(log)

    private var loadingGraphic: ImageView by singleAssign()

    private val platformProperty = ThreadAwareObjectProperty<Platform?>()
    var platform by platformProperty

    private val providerLogoProperty = ThreadAwareObjectProperty<Image?>()
    var providerLogo by providerLogoProperty

    val runningProperty = SimpleBooleanProperty(false)

    fun start() {
        _result = async(CommonPool) {
            this@Task._context = coroutineContext

            run(JavaFx) {
                runningProperty.set(true)
                MainView.showPersistentNotification(GridPane().apply {
                    paddingAll = 10.0
                    hgap = 10.0
                    vgap = 5.0
                    row {
                        label(titleProperty) {
                            minWidth = 170.0
                        }
                        progressbar(progress.progressProperty) {
                            prefWidth = screenBounds.width
                            gridpaneConstraints { hAlignment = HPos.CENTER }
                        }
                        button(graphic = Images.error.toImageView().apply { fitWidth = 30.0; fitHeight = 30.0; isPreserveRatio = true }) {
                            setOnAction { _result.cancel() }
                        }
                    }
                    gridpane {
                        gridpaneConstraints { columnRowIndex(0, 1); hAlignment = HPos.LEFT; vAlignment = VPos.CENTER }
                        alignment = Pos.CENTER_LEFT
                        hgap = 5.0
                        fun redraw() {
                            this@gridpane.replaceChildren {
                                row {
                                    platform?.let {
                                        children += it.toLogo(38.0)
                                    }
                                    providerLogo?.let {
                                        imageview(it) {
                                            fitWidth = 160.0
                                            fitHeight = 40.0
                                            isPreserveRatio = true
                                        }
                                    }
                                }
                            }
                        }

                        platformProperty.perform { redraw() }
                        providerLogoProperty.perform { redraw() }
                    }
                    text(progress.messageProperty) {
                        gridpaneConstraints { columnRowIndex(1, 1); hAlignment = HPos.CENTER }
                    }
                    loadingGraphic = imageview(Images.loading) {
                        gridpaneConstraints { columnRowIndex(2, 1); }
                        fitWidth = 40.0
                        fitHeight = 40.0
                        isPreserveRatio = true
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
                    loadingGraphic.image = Images.tick
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