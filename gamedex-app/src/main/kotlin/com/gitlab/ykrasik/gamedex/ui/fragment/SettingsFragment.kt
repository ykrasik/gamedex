package com.gitlab.ykrasik.gamedex.ui.fragment

import com.gitlab.ykrasik.gamedex.GameProviderType
import com.gitlab.ykrasik.gamedex.controller.SettingsController
import com.gitlab.ykrasik.gamedex.repository.GameProviderRepository
import com.gitlab.ykrasik.gamedex.settings.AllSettings
import com.gitlab.ykrasik.gamedex.settings.GameSettings
import com.gitlab.ykrasik.gamedex.settings.ProviderSettings
import com.gitlab.ykrasik.gamedex.ui.*
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Glow
import javafx.scene.layout.Pane
import org.controlsfx.glyphfont.FontAwesome
import tornadofx.*


/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class SettingsFragment : Fragment("Settings") {
    private val settingsController: SettingsController by di()
    private val settings: AllSettings by di()
    private val providerRepository: GameProviderRepository by di()

    // TODO: Use a viewModel.
    override val root = borderpane {
        top {
            toolbar {
                acceptButton { setOnAction { close() } }
//                verticalSeparator()
//                spacer()
//                verticalSeparator()
//                cancelButton { setOnAction { close() } }
            }
        }
        center {
            tabpane {
                nonClosableTab("General") {
                    vbox(spacing = 10.0) {
                        paddingAll = 40
                        button("Export Database") {
                            setOnAction {
                                settingsController.exportDatabase()
                            }
                        }
                        button("Import Database") {
                            setOnAction {
                                settingsController.importDatabase()
                            }
                        }
                        // TODO: Add a 'purge images' button
                    }
                }
                nonClosableTab("Game Provider") {
                    form {
                        paddingAll = 40
                        // TODO: Consider ControlsFx PropertySheet
                        fieldset("Provider Order") {
                            listOf(
                                "Search" to settings.provider.searchOrderProperty,
                                "Name" to settings.provider.nameOrderProperty,
                                "Description" to settings.provider.descriptionOrderProperty,
                                "Release Date" to settings.provider.releaseDateOrderProperty,
                                "Critic Score" to settings.provider.criticScoreOrderProperty,
                                "User Score" to settings.provider.userScoreOrderProperty,
                                "Thumbnail" to settings.provider.thumbnailOrderProperty,
                                "Poster" to settings.provider.posterOrderProperty,
                                "Screenshots" to settings.provider.screenshotOrderProperty
                            ).forEach { (name, orderProperty) ->
                                field(name) {
                                    providerOrder(orderProperty)
                                }
                            }
                        }
                    }
                }
                nonClosableTab("Game Display") {
                    form {
                        paddingAll = 40
                        fieldset("Game Display Type") {
                            field("Type") { enumComboBox(settings.game.displayTypeProperty) }
                        }

                        fieldset("Game Wall") {
                            visibleWhen { settings.game.displayTypeProperty.isEqualTo(GameSettings.DisplayType.wall) }
                            field("Cell Image Display") { enumComboBox(settings.gameWall.imageDisplayTypeProperty) }
                            // TODO: Should probably validate this data.
                            field("Cell Width") { adjustableTextField(settings.gameWall.cellWidthProperty) }
                            field("Cell Height") { adjustableTextField(settings.gameWall.cellHeightProperty) }
                            field("Cell Horizontal Spacing") { adjustableTextField(settings.gameWall.cellHorizontalSpacingProperty) }
                            field("Cell Vertical Spacing") { adjustableTextField(settings.gameWall.cellVerticalSpacingProperty) }
                        }
                    }
                }
            }
        }
    }

    private fun Pane.providerOrder(orderProperty: ObjectProperty<ProviderSettings.Order>) {
        hbox(spacing = 20.0) {
            alignment = Pos.CENTER
            orderProperty.perform { order ->
                var dragging: GameProviderType? = null
                replaceChildren {
                    order.ordered().map { provider ->
                        label {
                            addClass(Style.providerOrderLabel)
                            graphic = imageview(providerRepository.logo(provider)) {
                                fitWidth = 100.0
                                fitHeight = 50.0
                                isPreserveRatio = true
                            }
                            userData = provider

                            val dropShadow = DropShadow()
                            val glow = Glow()
                            effect = dropShadow

                            var dragX = 0.0
                            
                            setOnMousePressed { mouseEvent ->
                                // record a delta distance for the drag and drop operation.
                                dragX = layoutX - mouseEvent.sceneX
                                cursor = Cursor.MOVE
                                dragging = provider
                                this@hbox.children.forEach { it.isManaged = false }
                            }
                            setOnMouseReleased {
                                cursor = Cursor.HAND
                                dragging = null
                                this@hbox.children.forEach { it.isManaged = true }
                            }
                            setOnMouseDragged { mouseEvent ->
                                layoutX = mouseEvent.sceneX + dragX
                                val intersect = this@hbox.children.find { label ->
                                    this@label != label && this@label.boundsInParent.intersects(label.boundsInParent)
                                }
                                if (intersect != null) {
                                    orderProperty.value = order.switch(
                                        dragging!!,
                                        intersect.userData as GameProviderType
                                    )
                                }
                            }
                            setOnMouseEntered {
                                cursor = Cursor.HAND
                                dropShadow.input = glow
                            }
                            setOnMouseExited {
                                dropShadow.input = null
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Pane.adjustableTextField(property: Property<Double>) {
        val textfield = textfield(property)

        fun adjustButton(icon: FontAwesome.Glyph, adjustment: Int) {
            button(graphic = icon.toGraphic()) {
                setOnAction {
                    textfield.text = ((textfield.text).toDouble() + adjustment).toString()
                }
            }
        }

        adjustButton(FontAwesome.Glyph.PLUS, +1)
        adjustButton(FontAwesome.Glyph.MINUS, -1)
    }

    fun show() {
        openModal(block = true)
    }

    class Style : Stylesheet() {
        companion object {
            val providerOrderLabel by cssclass()

            init {
                importStylesheet(Style::class)
            }
        }

        init {
            providerOrderLabel {
                prefWidth = 100.px
                alignment = Pos.BASELINE_CENTER
            }
        }
    }
}
//fun SettingsScreen(@NonNull configService: ConfigService, @NonNull stageManager: StageManager): ??? {
//    this.configService = configService
//    this.stageManager = stageManager
//
//    val loader = FXMLLoader(UIResources.settingsScreenFxml())
//    loader.setController(this)
//    val root = loader.load()
//
//    val scene = Scene(root, Color.TRANSPARENT)
//    scene.stylesheets.addAll(UIResources.mainCss(), UIResources.settingsScreenCss())
//
//    stage.setWidth(600.0)
//    stage.initStyle(StageStyle.TRANSPARENT)
//    stage.initModality(Modality.APPLICATION_MODAL)
//    stage.setScene(scene)
//
//    // Make the stage draggable by clicking anywhere.
//    JavaFxUtils.makeStageDraggable(stage, root)
//}