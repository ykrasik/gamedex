package com.gitlab.ykrasik.gamedex.ui.view.fragment
import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.cancelButton
import com.gitlab.ykrasik.gamedex.ui.okButton
import com.gitlab.ykrasik.gamedex.ui.verticalSeparator
import com.gitlab.ykrasik.gamedex.ui.view.widgets.ImageViewResizingPane
import javafx.scene.image.ImageView
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import javafx.stage.StageStyle
import tornadofx.*

/**
 * User: ykrasik
 * Date: 30/03/2017
 * Time: 18:17
 */
class GameDetailsFragment(game: Game) : Fragment(game.name) {
    private val imageLoader: ImageLoader by di()

    private var accept = false

    override val root = borderpane {
        top {
            vbox {
                buttonbar {
                    minHeight = 80.0
                    okButton { setOnAction { close(accept = true) } }
                    cancelButton { setOnAction { close(accept = false) } }

                    button("Change Thumbnail")
                    button("Change Poster")
                    button("Refresh")
                    button("Search Again")
                    button("Delete")
                }
                separator()
            }
        }
        center {
            hbox {
                stackpane {
                    style {
                        styleClass += "card"     // TODO: Type-safety!
                    }
                    paddingAll = 5

                    val screenWidth = Screen.getPrimary().bounds.width

                    val poster = ImageView()
                    val posterPane = ImageViewResizingPane(poster)  // TODO: Add syntactic sugar for this.
                    posterPane.maxWidth = screenWidth * maxPosterWidthPercent

                    // Clip the posterPane's corners to be round after the posterPane's size is calculated.
                    val clip = Rectangle()
                    clip.arcWidth = 20.0
                    clip.arcHeight = 20.0
                    posterPane.heightProperty().onChange { clip.height = it }
                    posterPane.widthProperty().onChange { clip.width = it }
                    posterPane.clip = clip

                    poster.imageProperty().bind(imageLoader.fetchImage(game.imageIds.posterId))

                    children += posterPane
                }
                verticalSeparator(padding = 10.0)
            }
        }
    }

    override fun onDock() {
        modalStage!!.isMaximized = true
    }

    fun show(): Boolean {
        openModal(block = true, stageStyle = StageStyle.TRANSPARENT)
        return accept
    }

    private fun close(accept: Boolean) {
        this.accept = accept
        close()
    }

    companion object {
        private val maxPosterWidthPercent = 0.5
    }
}

//
//<BorderPane fx:id="container" xmlns="http://javafx.com/javafx/8" xmlns:fx="http://javafx.com/fxml/1">
//<center>
//<HBox>
//<children>
//<StackPane fx:id="posterContainer" styleClass="card" HBox.hgrow="SOMETIMES">
//<BorderPane.margin>
//<Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
//</BorderPane.margin>
//<HBox.margin>
//<Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
//</HBox.margin>
//</StackPane>
//<VBox alignment="TOP_RIGHT" styleClass="card" HBox.hgrow="ALWAYS">
//<children>
//<GridPane fx:id="attributes" alignment="TOP_RIGHT" hgap="5.0" vgap="8.0" BorderPane.alignment="CENTER" StackPane.alignment="CENTER_RIGHT">
//<children>
//<Label text="Description:" GridPane.rowIndex="4" />
//<TextArea fx:id="description" wrapText="true" GridPane.columnIndex="1" GridPane.hgrow="ALWAYS" GridPane.rowIndex="4" GridPane.vgrow="ALWAYS" />
//<Label text="Platform:" GridPane.rowIndex="5" />
//<TextField fx:id="platform" editable="false" GridPane.columnIndex="1" GridPane.rowIndex="5" />
//<Label text="Release Date:" GridPane.rowIndex="6" />
//<TextField fx:id="releaseDate" editable="false" GridPane.columnIndex="1" GridPane.rowIndex="6" />
//<Label text="Critic Score:" GridPane.rowIndex="7" />
//<HBox fx:id="criticScoreContainer" GridPane.columnIndex="1" GridPane.rowIndex="7">
//<children>
//<TextField fx:id="criticScore" GridPane.columnIndex="1" GridPane.rowIndex="6" HBox.hgrow="ALWAYS">
//<HBox.margin>
//<Insets right="20.0" />
//</HBox.margin>
//</TextField>
//</children>
//</HBox>
//<Label text="User Score:" GridPane.rowIndex="8" />
//<TextField fx:id="userScore" GridPane.columnIndex="1" GridPane.rowIndex="8" />
//<TextField fx:id="genres" GridPane.columnIndex="1" GridPane.rowIndex="9" />
//<Label text="Genres:" GridPane.rowIndex="9" />
//<Label text="URL:" GridPane.rowIndex="10" />
//<Hyperlink fx:id="url" GridPane.columnIndex="1" GridPane.rowIndex="10" />
//<Label id="nameLabel" fx:id="nameLabel" text="Name" wrapText="true" GridPane.columnSpan="2" GridPane.halignment="CENTER" GridPane.rowIndex="1" GridPane.valignment="CENTER" />
//<Label id="pathLabel" fx:id="pathLabel" text="Path" wrapText="true" GridPane.columnSpan="2" GridPane.halignment="CENTER" GridPane.rowIndex="3" GridPane.valignment="CENTER" />
//</children>
//<columnConstraints>
//<ColumnConstraints fillWidth="false" halignment="RIGHT" hgrow="SOMETIMES" minWidth="-Infinity" />
//<ColumnConstraints halignment="LEFT" hgrow="SOMETIMES" />
//</columnConstraints>
//<rowConstraints>
//<RowConstraints fillHeight="false" maxHeight="1.7976931348623157E308" minHeight="-Infinity" vgrow="ALWAYS" />
//<RowConstraints fillHeight="false" minHeight="10.0" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" minHeight="100.0" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints minHeight="-Infinity" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//<RowConstraints fillHeight="false" vgrow="SOMETIMES" />
//</rowConstraints>
//<BorderPane.margin>
//<Insets bottom="50.0" left="50.0" right="50.0" top="50.0" />
//</BorderPane.margin>
//</GridPane>
//</children>
//<padding>
//<Insets bottom="10.0" left="10.0" right="10.0" top="10.0" />
//</padding>
//<HBox.margin>
//<Insets bottom="10.0" right="10.0" top="10.0" />
//</HBox.margin>
//</VBox>
//</children>
//</HBox>
//</center>
//</BorderPane>