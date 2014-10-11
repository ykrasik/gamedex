package com.github.ykrasik.indexter.games;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Yevgeny Krasik
 */
public class MainWindowController implements Initializable {
    @FXML
    private TilePane gameWall;

    @FXML
    private VBox gameDetails;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    // FIXME: This is abuse, not what the controller is meant to do.
    public TilePane getGameWall() {
        return gameWall;
    }

    public VBox getGameDetails() {
        return gameDetails;
    }
}
