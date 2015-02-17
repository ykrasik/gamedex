package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.core.config.GameCollectionConfig;
import com.github.ykrasik.gamedex.core.flow.FlowManager;
import com.github.ykrasik.gamedex.core.game.GameManager;
import com.github.ykrasik.gamedex.core.library.LibraryManager;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.StatusBar;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
// TODO: Allow changing thumbnail & poster via right-click.
// TODO: Add detail view on double click
// TODO: Add right-click menus to library list.
// TODO: Photos should be streamed, and only fetched from DB when accessed. Especially posters.
// TODO: Log should be a splitPane.
// TODO: Add ability to have gamePacks.
@Slf4j
@RequiredArgsConstructor
public class MainController implements Controller {
    @FXML private MenuItem addLibraryMenuItem;

    @FXML private SplitPane content;
    private double dividerPosition;

    @FXML private VBox bottomContainer;
    @FXML private StatusBar statusBar;
    @FXML private Label gameCount;
    @FXML private Label libraryCount;
    @FXML private ToggleButton toggleLog;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button statusBarStopButton;
    @FXML private TextArea logTextArea;

    @NonNull private final Stage stage;
    @NonNull private final GameCollectionConfig config;
    @NonNull private final FlowManager flowManager;
    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;

    // Called by JavaFx
    public void initialize() {
        initMenu();
        initBottom();
    }

    private void initMenu() {
        addLibraryMenuItem.setOnAction(e -> addLibrary());
//        showSideBar.selectedProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue) {
//                contentScreen.getChildren().add(sideBar);
//            } else {
//                contentScreen.getChildren().remove(sideBar);
//            }
//        });
    }

    private void initBottom() {
        progressIndicator.progressProperty().bind(flowManager.fetchProgressProperty());
        statusBar.progressProperty().bind(flowManager.progressProperty());
        statusBar.textProperty().bind(flowManager.messageProperty());
        flowManager.messageProperty().addListener((observable, oldValue, newValue) -> {
            logTextArea.appendText(newValue);
            logTextArea.appendText("\n");
        });

        dividerPosition = content.getDividerPositions()[0];
        toggleLog.selectedProperty().addListener((observable, oldValue, newValue) -> toggleLogTextArea(newValue));

        gameCount.textProperty().bind(gameManager.gamesProperty().sizeProperty().asString("Games: %d"));
        libraryCount.textProperty().bind(libraryManager.librariesProperty().sizeProperty().asString("Libraries: %d"));
    }

    private void toggleLogTextArea(boolean newValue) {
        if (newValue) {
            content.getItems().add(logTextArea);
            content.setDividerPositions(dividerPosition);
        } else {
            dividerPosition = content.getDividerPositions()[0];
            content.getItems().remove(logTextArea);
        }
    }

//    private ContextMenu createLibraryContextMenu() {
//        final ContextMenu contextMenu = new ContextMenu();
//
//        final MenuItem deleteItem = new MenuItem("Delete");
//        deleteItem.setOnAction(e -> {
//            final Object source = e.getSource();
//            final EventTarget target = e.getTarget();
////            final LocalGame game = cell.getItem();
////            libraryManager.deleteLibrary(game);
//        });
//
//        contextMenu.getItems().addAll(deleteItem);
//        return contextMenu;
//    }

    // FIXME: Move this into FlowManager.
    private void addLibrary() {
        final DirectoryChooser directoryChooser = createDirectoryChooser("Add Library");
        final File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            config.setPrevDirectory(selectedDirectory);
            final Path path = Paths.get(selectedDirectory.toURI());
            try {
                if (libraryManager.isLibrary(path)) {
                    throw new GameDexException("Already have a library defined for '%s'", path);
                }

                // FIXME: Show a select name dialog too.
                final Optional<GamePlatform> platform = createDialog()
                    .title("Choose library platform")
                    .masthead(path.toString())
                    .message("Choose library platform:")
                    .showChoices(GamePlatform.values());
                platform.ifPresent(p -> libraryManager.createLibrary(path.getFileName().toString(), path, p));
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    @FXML
    public void refreshLibraries() {
        prepareTask(flowManager.refreshLibraries());
    }

    @FXML
    public void cleanupGames() {
        prepareTask(flowManager.cleanupGames());
    }

    private void prepareTask(Task<Void> task) {
        task.setOnFailed(event -> handleException(task.getException()));
        task.setOnCancelled(event -> flowManager.stopTask(task));

        progressIndicator.visibleProperty().bind(task.runningProperty());

        statusBarStopButton.disableProperty().bind(task.runningProperty().not());
        statusBarStopButton.visibleProperty().bind(task.runningProperty());
        statusBarStopButton.setOnAction(e -> task.cancel());

        // TODO: Disable all other buttons while task is running.
    }

    private void handleException(Throwable t) {
        log.warn("Error cleaning up games:", t);
        createDialog().title("Error:").message(t.getMessage()).showException(t);
    }

    private DirectoryChooser createDirectoryChooser(String title) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(config.getPrevDirectory().getOrElseNull());
        return directoryChooser;
    }

    private Dialogs createDialog() {
        return Dialogs.create().owner(stage);
    }
}
