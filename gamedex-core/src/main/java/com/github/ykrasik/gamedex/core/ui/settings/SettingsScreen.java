package com.github.ykrasik.gamedex.core.ui.settings;

import com.github.ykrasik.gamedex.common.util.JavaFxUtils;
import com.github.ykrasik.gamedex.core.config.ConfigType;
import com.github.ykrasik.gamedex.core.config.ConfigService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.core.ui.gridview.GameWallImageDisplay;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * @author Yevgeny Krasik
 */
public class SettingsScreen {
    private final ConfigService configService;
    private final Stage stage;

    @FXML private ComboBox<GameWallImageDisplay> gameWallImageDisplayComboBox;

    public SettingsScreen(@NonNull ConfigService configService) {
        this.configService = configService;
        this.stage = JavaFxUtils.returnLaterIfNecessary(this::createStage);
    }

    @SneakyThrows
    private Stage createStage() {
        final Stage stage = new Stage();

        final FXMLLoader loader = new FXMLLoader(UIResources.settingsScreenFxml());
        loader.setController(this);
        final BorderPane root = loader.load();

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.settingsScreenCss());

        // Make the stage draggable by clicking anywhere.
        JavaFxUtils.makeDraggable(stage, root);

        stage.setWidth(600);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        return stage;
    }

    @FXML
    private void initialize() {
        initGameWallImageDisplay();
    }

    private void initGameWallImageDisplay() {
        gameWallImageDisplayComboBox.setItems(FXCollections.observableArrayList(GameWallImageDisplay.values()));

        gameWallImageDisplayComboBox.getSelectionModel().select(gameWallImageDisplayProperty().get());
        gameWallImageDisplayProperty().bind(gameWallImageDisplayComboBox.getSelectionModel().selectedItemProperty());
    }

    public void show() {
        stage.showAndWait();
    }

    @FXML
    public void close() {
        stage.close();
    }

    private ObjectProperty<GameWallImageDisplay> gameWallImageDisplayProperty() {
        return configService.property(ConfigType.GAME_WALL_IMAGE_DISPLAY);
    }
}
