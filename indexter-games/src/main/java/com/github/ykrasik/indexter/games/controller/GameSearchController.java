package com.github.ykrasik.indexter.games.controller;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.Library;
import com.github.ykrasik.indexter.games.logic.ChoiceProvider;
import com.github.ykrasik.indexter.games.logic.LogicManager;
import com.github.ykrasik.indexter.games.logic.DialogChoiceProvider;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.controlsfx.control.StatusBar;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Yevgeny Krasik
 */
// FIXME: This class is redundant.
public class GameSearchController extends AbstractService {
    private final LogicManager logicManager;
    private final ChoiceProvider choiceProvider;

    private final StatusBar statusBar;
    private final Button stopRefreshButton = new Button("Stop");

    private ExecutorService executorService;

    public GameSearchController(Stage stage, StatusBar statusBar, LogicManager logicManager) {
        this.logicManager = Objects.requireNonNull(logicManager);
        this.choiceProvider = new DialogChoiceProvider(Objects.requireNonNull(stage));

        this.statusBar = Objects.requireNonNull(statusBar);
        statusBar.textProperty().bind(logicManager.messageProperty());
        // FIXME: Doesn't work.
        statusBar.progressProperty().bind(logicManager.refreshLibraryProgressProperty());

        statusBar.getRightItems().add(stopRefreshButton);
        stopRefreshButton.setDisable(true);
        stopRefreshButton.setCancelButton(true);
    }

    @Override
    protected void doStart() throws Exception {
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void doStop() throws Exception {
        executorService.shutdownNow();
    }

    public void refreshLibraries() throws Exception {
        final Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                logicManager.refreshLibraries(choiceProvider);
                return null;
            }
        };
        submit(task);
    }

    public void scanDirectory(Path root, GamePlatform platform) throws Exception {
        final Library tempLibrary = new Library("tempScan", root, platform);
        final Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                logicManager.refreshLibrary(tempLibrary, choiceProvider);
                return null;
            }
        };
        submit(task);
    }

    public void processPath(Path path, GamePlatform platform) throws Exception {
        final Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                logicManager.processPath(path, platform, choiceProvider);
                return null;
            }
        };
        submit(task);
    }

    private void submit(Task<?> task) {
        task.setOnFailed(v -> { throw new RuntimeException(task.getException()); });
        linkStopButton(task);
        executorService.submit(task);
    }

    private void linkStopButton(Task<?> task) {
        stopRefreshButton.disableProperty().bind(task.runningProperty().not());
        stopRefreshButton.setOnAction(e -> {
            task.cancel();
            logicManager.messageProperty().set("Cancelled");
        });
    }
}
