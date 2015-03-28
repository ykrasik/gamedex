package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.core.controller.game.GameController;
import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.javafx.MoreBindings;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.manager.library.LibraryManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.screen.settings.SettingsScreen;
import javafx.beans.binding.Binding;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.StatusBar;

/**
 * @author Yevgeny Krasik
 */
// TODO: Allow changing thumbnail & poster via right-click.
// TODO: Add right-click menus to library list.
// TODO: Add ability to have gamePacks.
@Slf4j
@RequiredArgsConstructor
public class MainController implements Controller {
    @FXML private MenuItem addLibraryMenuItem;

    @FXML private SplitPane content;

    @FXML private VBox bottomContainer;
    @FXML private StatusBar statusBar;
    @FXML private Label gameCount;
    @FXML private Label libraryCount;
    @FXML private ToggleButton toggleLog;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Button statusBarStopButton;
    @FXML private TextArea logTextArea;

    @FXML private GameController gamesController;

    @NonNull private final ConfigService configService;
    @NonNull private final ActionService actionService;

    @NonNull private final GameManager gameManager;
    @NonNull private final LibraryManager libraryManager;

    @NonNull private final SettingsScreen settingsScreen;

    @FXML
    public void initialize() {
        initMenu();
        initBottom();
    }

    private void initMenu() {
        addLibraryMenuItem.setOnAction(e -> actionService.addNewLibrary());
    }

    private void initBottom() {
        final Binding<Double> binding = MoreBindings.transformBinding(actionService.fetchingProperty(), fetching -> (double) (fetching ? -1 : 0));
        progressIndicator.progressProperty().bind(binding);

        statusBar.progressProperty().bind(actionService.progressProperty());

        actionService.messageProperty().addListener((observable, oldValue, newValue) -> JavaFxUtils.runLaterIfNecessary(() -> {
            if (newValue != null) {
                statusBar.setText(newValue);
                logTextArea.appendText(newValue);
                logTextArea.appendText("\n");
            }
        }));
//        statusBar.textProperty().bind(actionService.messageProperty());

//        actionService.messageProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue != null) {
//                logTextArea.appendText(newValue);
//                logTextArea.appendText("\n");
//            }
//        });

        toggleLog.selectedProperty().addListener((observable, oldValue, newValue) -> toggleLogTextArea(newValue));
        toggleLog.selectedProperty().bindBidirectional(configService.showLogProperty());

        gameCount.textProperty().bind(gameManager.gamesProperty().sizeProperty().asString("Games: %d"));
        libraryCount.textProperty().bind(libraryManager.librariesProperty().sizeProperty().asString("Libraries: %d"));

        gamesController.currentTaskProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                registerCurrentTask(newValue);
            }
        });
    }

    private void toggleLogTextArea(boolean show) {
        if (show) {
            content.getItems().add(logTextArea);
            bindLogDivider();
        } else {
            content.getItems().remove(logTextArea);
        }
    }

    private void registerCurrentTask(Task<Void> task) {
        task.setOnCancelled(event -> actionService.stopTask(task));

        progressIndicator.visibleProperty().bind(task.runningProperty());

        statusBarStopButton.disableProperty().bind(task.runningProperty().not());
        statusBarStopButton.visibleProperty().bind(task.runningProperty());
        statusBarStopButton.setOnAction(e -> task.cancel());

        // TODO: Disable all other buttons while task is running.
    }

    private void bindLogDivider() {
        content.setDividerPosition(0, configService.getLogDividerPosition());
        configService.logDividerPosition().bind(content.getDividers().get(0).positionProperty());
    }

    @FXML
    public void cleanupGames() {
        actionService.cleanupGames();
    }

    @FXML
    public void showSettings() {
        settingsScreen.show();
    }
}
