package com.github.ykrasik.gamedex.core.ui.library;

import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.SneakyThrows;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public class CreateLibraryDialog {
    private final Stage stage;

    @FXML private Label pathLabel;
    @FXML private TextField libraryNameTextField;
    @FXML private ComboBox<GamePlatform> platformComboBox;
    @FXML private TreeView<Path> childrenTreeView;
    @FXML private Button yesButton;

    private Opt<LibraryDef> result = Opt.absent();

    public CreateLibraryDialog() {
        this.stage = JavaFxUtils.returnLaterIfNecessary(this::createStage);
    }

    @SneakyThrows
    private Stage createStage() {
        final Stage stage = new Stage();

        final FXMLLoader loader = new FXMLLoader(UIResources.libraryDialogFxml());
        loader.setController(this);
        final BorderPane root = loader.load();

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.libraryDialogCss());

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
        platformComboBox.setItems(FXCollections.observableArrayList(GamePlatform.values()));
    }

    public Opt<LibraryDef> show(Path path, ImmutableList<Path> children, GamePlatform defaultPlatform) {
        pathLabel.setText(path.toString());
        libraryNameTextField.setText(path.getFileName().toString());
        platformComboBox.getSelectionModel().select(defaultPlatform);

        final TreeItem<Path> root = new TreeItem<>(path);
        root.setExpanded(true);
        final ImmutableList<TreeItem<Path>> childTreeItems = children.collect(TreeItem::new);
        root.getChildren().addAll(childTreeItems.castToList());
        childrenTreeView.setRoot(root);

        yesButton.setOnAction(e -> {
            result = Opt.of(createFromInput(path));
            stage.hide();
        });

        result = Opt.absent();
        stage.showAndWait();
        return result;
    }

    @FXML
    public void close() {
        stage.close();
    }

    private LibraryDef createFromInput(Path path) {
        return new LibraryDef(path, libraryNameTextField.getText(), platformComboBox.getValue());
    }
}
