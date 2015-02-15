package com.github.ykrasik.gamedex.core.ui.library;

import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.core.ui.dialog.StageDragger;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;
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
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public class CreateLibraryDialog {
    private static final CreateLibraryDialog INSTANCE = new CreateLibraryDialog();

    private final Stage stage = new Stage();

    @FXML private Label pathLabel;
    @FXML private TextField libraryNameTextField;
    @FXML private ComboBox<GamePlatform> platformComboBox;
    @FXML private TreeView<Path> childrenTreeView;
    @FXML private Button yesButton;
    @FXML private Button noButton;

    private Opt<LibraryDef> result = Opt.absent();

    @SneakyThrows
    public CreateLibraryDialog() {
        final FXMLLoader loader = new FXMLLoader(UIResources.libraryDialogFxml());
        loader.setController(this);
        final BorderPane root = loader.load();

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().add(UIResources.libraryDialogCss());

        // Make the stage draggable by clicking anywhere.
        StageDragger.create(stage, root);

        stage.setWidth(500);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        platformComboBox.setItems(FXCollections.observableArrayList(GamePlatform.values()));
    }

    public Opt<LibraryDef> show(Path path, List<Path> children, GamePlatform defaultPlatform) {
        pathLabel.setText(path.toString());
        libraryNameTextField.setText(path.getFileName().toString());
        platformComboBox.getSelectionModel().select(defaultPlatform);

        final TreeItem<Path> root = new TreeItem<>(path);
        root.setExpanded(true);
        final List<TreeItem<Path>> childTreeItems = ListUtils.map(children, TreeItem::new);
        root.getChildren().addAll(childTreeItems);
        childrenTreeView.setRoot(root);

        result = Opt.absent();
        yesButton.setOnAction(e -> {
            result = Opt.of(createFromInput(path));
            stage.hide();
        });
        noButton.setOnAction(e -> stage.hide());

        stage.showAndWait();
        return result;
    }

    private LibraryDef createFromInput(Path path) {
        return new LibraryDef(libraryNameTextField.getText(), path, platformComboBox.getValue());
    }

    public static CreateLibraryDialog create() {
        return INSTANCE;
    }
}
