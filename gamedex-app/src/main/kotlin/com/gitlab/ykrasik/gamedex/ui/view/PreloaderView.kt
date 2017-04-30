package com.gitlab.ykrasik.gamedex.ui.view

import com.gitlab.ykrasik.gamedex.core.NotificationManager
import com.gitlab.ykrasik.gamedex.module.GuiceDiContainer
import com.gitlab.ykrasik.gamedex.util.ProgramData
import com.google.inject.AbstractModule
import com.google.inject.matcher.Matchers
import com.google.inject.spi.ProvisionListener
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
    private val notificationManager = NotificationManager()

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
                    progressBar.bind(notificationManager.progressProperty)
                }
                row {
                    label(notificationManager.messageProperty)
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
        notificationManager.message("Loading...")
        val programData = ProgramData.get()
        val provisionListener = GamedexProvisionListener(programData.amountOfDiComponents)

        FX.dicontainer = GuiceDiContainer(
            GuiceDiContainer.defaultModules + LifecycleModule(provisionListener) + NotificationModule()
        )

        notificationManager.message("Done loading.")
        // Save the total amount of DI components detected into a file, so next loading screen will be more accurate.
        ProgramData.write(programData.copy(amountOfDiComponents = provisionListener.componentCount))
    }

    private inner class NotificationModule : AbstractModule() {
        override fun configure() {
            bind(NotificationManager::class.java).toInstance(notificationManager)
        }
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
            notificationManager.progress(_componentCount, totalComponents)
        }
    }
}