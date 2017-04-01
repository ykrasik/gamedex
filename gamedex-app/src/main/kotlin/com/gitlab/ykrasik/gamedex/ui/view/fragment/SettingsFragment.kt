package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.enumComboBox
import com.gitlab.ykrasik.gamedex.ui.nonClosableTab
import com.gitlab.ykrasik.gamedex.ui.okButton
import com.gitlab.ykrasik.gamedex.util.UserPreferences
import tornadofx.*

/**
 * User: ykrasik
 * Date: 06/01/2017
 * Time: 22:22
 */
class SettingsFragment : Fragment() {
    private val userPreferences: UserPreferences by di()

    override val root = borderpane {
        center {
            tabpane {
                nonClosableTab("Game Wall") {
                    form {
                        fieldset("Cell Image Display") {
                            field("Type") { enumComboBox(userPreferences.gameWallImageDisplayTypeProperty) }
                        }
                    }
                }
            }
        }
        bottom {
            buttonbar {
                cancelButton { setOnAction { close() } }
                okButton { setOnAction { close() } }
            }
        }
    }

    fun show() {
        openModal(block = true)
    }
}
//
//<BorderPane xmlns="http://javafx.com/javafx/8" xmlns:fx="http://javafx.com/fxml/1" styleClass="screen">
//<center>
//<TabPane minHeight="-Infinity" minWidth="-Infinity" tabClosingPolicy="UNAVAILABLE" BorderPane.alignment="CENTER">
//<tabs>
//<Tab closable="false" text="Game Wall">
//<content>
//<GridPane hgap="10.0" vgap="10.0">
//<columnConstraints>
//<ColumnConstraints halignment="RIGHT" hgrow="SOMETIMES" minWidth="-Infinity" />
//<ColumnConstraints halignment="LEFT" hgrow="SOMETIMES" minWidth="-Infinity" />
//</columnConstraints>
//<rowConstraints>
//<RowConstraints minHeight="-Infinity" vgrow="SOMETIMES" />
//<RowConstraints minHeight="-Infinity" vgrow="SOMETIMES" />
//<RowConstraints minHeight="-Infinity" vgrow="SOMETIMES" />
//</rowConstraints>
//<children>
//<Label text="Wall Cell Image Behavior">
//<tooltip>
//<Tooltip autoHide="true" text="How to handle images that don't exactly fit a cell" />
//</tooltip>
//</Label>
//<ComboBox fx:id="gameWallImageDisplayComboBox" GridPane.columnIndex="1" />
//</children>
//<padding>
//<Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
//</padding>
//</GridPane>
//</content>
//</Tab>
//</tabs>
//</TabPane>
//</center>
//<bottom>
//<Button fx:id="closeButton" cancelButton="true" defaultButton="true" minHeight="40.0" minWidth="40.0" mnemonicParsing="false" onAction="#close" text="Close" BorderPane.alignment="CENTER_RIGHT">
//<BorderPane.margin>
//<Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
//</BorderPane.margin>
//</Button>
//</bottom>
//</BorderPane>
//
//
//private val configService: ConfigService
//private val stageManager: StageManager
//
//@FXML private val gameWallImageDisplayComboBox: ComboBox<GameWallImageDisplay>? = null
//
//@SneakyThrows
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
//
//@FXML
//private fun initialize() {
//    initGameWallImageDisplay()
//}
//
//private fun initGameWallImageDisplay() {
//    gameWallImageDisplayComboBox!!.setItems(FXCollections.observableArrayList<GameWallImageDisplay>(*GameWallImageDisplay.values()))
//
//    gameWallImageDisplayComboBox.getSelectionModel().select(configService.getGameWallImageDisplay())
//    configService.gameWallImageDisplayProperty().bind(gameWallImageDisplayComboBox.getSelectionModel().selectedItemProperty())
//}