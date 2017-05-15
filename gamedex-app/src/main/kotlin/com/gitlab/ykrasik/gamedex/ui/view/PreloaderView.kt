package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.core.TaskProgress
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.settings.GeneralSettings
import com.gitlab.ykrasik.gamedex.util.Log
import com.gitlab.ykrasik.gamedex.util.LogEntry
import com.google.inject.AbstractModule
import com.google.inject.matcher.Matchers
import com.google.inject.spi.ProvisionListener
import javafx.collections.ListChangeListener
import javafx.scene.effect.DropShadow
import javafx.stage.Screen
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import tornadofx.*

/**
 * User: ykrasik
 * Date: 01/04/2017
 * Time: 21:53
 */
class PreloaderView : View("Gamedex") {
    private var logo = resources.image("gamedex.png")
    private val progress = TaskProgress(log = null)

    private val messageListener = ListChangeListener<LogEntry> {
        progress.message = it.list.last().message
    }

    init {
        // TODO: Could consider doing this through a task.
        // While loading, flush all log messages to the notification.
        Log.entries.addListener(messageListener)
    }

    override val root = borderpane {
        center {
            imageview {
                image = logo
            }
        }
        bottom {
            gridpane {
                vgap = 5.0
                row {
                    val progressBar = progressbar(0.0) { prefWidth = logo.width }
                    progressBar.bind(progress.progressProperty)
                }
                row {
                    label(progress.messageProperty)
                }
            }
        }

        // TODO: Stylesheet.
        style = "-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: linear-gradient(to bottom, chocolate, derive(chocolate, 50%));"
        effect = DropShadow()
    }

    override fun onDock() {
//        primaryStage.initStyle(StageStyle.UNDECORATED)
        val bounds = Screen.getPrimary().bounds
        primaryStage.x = bounds.minX + bounds.width / 2 - logo.width / 2
        primaryStage.y = bounds.minY + bounds.height / 3 - logo.height / 2

        launch(CommonPool) {
            loadGamdex()
            run(JavaFx) {
                replaceWith(MainView::class)
            }
        }
    }

    private fun loadGamdex() {
        progress.message = "Loading..."

        // TODO: Meh, not super clean, but I'm not super bothered
        val preferences = GeneralSettings()
        val provisionListener = GamedexProvisionListener(preferences.amountOfDiComponents)

        FX.dicontainer = GuiceDiContainer(
            GuiceDiContainer.defaultModules + LifecycleModule(provisionListener)
        )

        progress.message = "Done loading."
        Log.entries.removeListener(messageListener)

        // Save the total amount of DI components detected into a file, so next loading screen will be more accurate.
        preferences.amountOfDiComponents = provisionListener.componentCount
    }


    private class LifecycleModule(private val listener: GamedexProvisionListener) : AbstractModule() {
        override fun configure() {
            bindListener(Matchers.any(), listener)
        }
    }

    private inner class GamedexProvisionListener(private val totalComponents: Int) : ProvisionListener {
        private var _componentCount = 0
        val componentCount: Int get() = _componentCount

        override fun <T : Any> onProvision(provision: ProvisionListener.ProvisionInvocation<T>) {
            _componentCount++
            progress.progress(_componentCount, totalComponents)
        }
    }
}