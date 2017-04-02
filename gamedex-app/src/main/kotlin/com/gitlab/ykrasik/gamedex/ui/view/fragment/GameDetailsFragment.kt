package com.gitlab.ykrasik.gamedex.ui.view.fragment

import com.gitlab.ykrasik.gamedex.common.datamodel.Game
import com.gitlab.ykrasik.gamedex.core.ImageLoader
import com.gitlab.ykrasik.gamedex.ui.*
import com.gitlab.ykrasik.gamedex.ui.view.Styles
import com.gitlab.ykrasik.gamedex.ui.view.widgets.ImageViewResizingPane
import javafx.scene.image.ImageView
import javafx.scene.layout.Priority
import javafx.scene.shape.Rectangle
import javafx.stage.Screen
import tornadofx.*
import java.net.URLEncoder

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
                    padding { right = 10; left = 10 }
                    minHeight = 40.0
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
            paddingAll = 10
            val screenWidth = Screen.getPrimary().bounds.width
            hbox {
                // Left
                stackpane {
                    addClass(Styles.card)       // TODO: Not sure what this does

                    val poster = ImageView()
                    poster.imageProperty().bind(imageLoader.fetchImage(game.imageIds.posterId))
                    
                    val posterPane = ImageViewResizingPane(poster)  // TODO: Add syntactic sugar for this.
                    posterPane.maxWidth = screenWidth * maxPosterWidthPercent

                    // Clip the posterPane's corners to be round after the posterPane's size is calculated.
                    val clip = Rectangle()
                    clip.arcWidth = 20.0
                    clip.arcHeight = 20.0
                    posterPane.heightProperty().onChange { clip.height = it }
                    posterPane.widthProperty().onChange { clip.width = it }
                    posterPane.clip = clip

                    children += posterPane
                }

                verticalSeparator(padding = 10.0)

                // Right
                vbox {
                    hgrow = Priority.ALWAYS
                    form {
                        fieldset {
                            field("Path") { readOnlyTextField(game.path.path) }
                            field("Name") { readOnlyTextField(game.name) }
                            field("Description") { readOnlyTextArea(game.description) { isWrapText = true } }
                            field("Release Date") { readOnlyTextField(game.releaseDate.toString()) }
                            field("Critic Score") { readOnlyTextField(game.criticScore.toString()) }
                            field("User Score") { readOnlyTextField(game.userScore.toString()) }
                            field("Genres") { readOnlyTextField(game.genres.joinToString(", ")) }
//                            field("URL") { hyperlink(game.u) }
                        }
                    }
                    separator { padding { top = 10; bottom = 10 } }
                    webview {
                        vgrow = Priority.ALWAYS
                        val search = URLEncoder.encode("${game.name} pc gameplay", "utf-8")
                        val url = "https://www.youtube.com/results?search_query=$search"
                        engine.load(url)
                    }
                }
            }
        }
    }

    override fun onDock() {
        modalStage!!.isMaximized = true
    }

    fun show(): Boolean {
        openModal(block = true, owner = null)
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