package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.ui.library.CreateLibraryDialog;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.yava.javafx.JavaFxUtils;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
public class DialogServiceImpl implements DialogService {
    private final Stage stage;
    private final StageManager stageManager;
    private final ConfigService configService;

    private final CreateLibraryDialog createLibraryDialog;

    public DialogServiceImpl(@NonNull Stage stage, @NonNull StageManager stageManager, @NonNull ConfigService configService) {
        this.stage = stage;
        this.stageManager = stageManager;
        this.configService = configService;

        this.createLibraryDialog = JavaFxUtils.callLaterIfNecessary(CreateLibraryDialog::new);
    }

    @Override
    public void showException(Throwable t) {
        log.warn("Error:", t);
        final Alert alert = initAlert(DialogFactory.createExceptionDialog(t));
        stageManager.callWithBlur(alert::showAndWait);
    }

    @Override
    public boolean confirmationDialog(String format, Object...args) {
        final Alert alert = initAlert(new Alert(AlertType.CONFIRMATION));
        alert.setTitle("Are you sure?");
        alert.setHeaderText(String.format(format, args));

        final Optional<ButtonType> result = stageManager.callWithBlur(alert::showAndWait);
        return (result.get() == ButtonType.OK);
    }

    @Override
    public <T> boolean confirmationListDialog(ObservableList<T> list, Function<T, String> stringifier, String format, Object... args) {
        final Alert alert = initAlert(DialogFactory.createConfirmationListDialog(String.format(format, args), list, stringifier));
        final Optional<ButtonType> result = stageManager.callWithBlur(alert::showAndWait);
        return (result.get() == ButtonType.OK);
    }

    @Override
    public Opt<LibraryDef> addLibraryDialog() {
        return chooseDirectory("Add Library").flatMap(this::createLibraryFromPath);
    }

    @SneakyThrows
    private Opt<LibraryDef> createLibraryFromPath(Path path) {
        final ImmutableList<Path> children = FileUtils.listFirstChildDirectories(path, 10).newWith(Paths.get("..."));
        return createLibraryDialog(path, children, GamePlatform.PC);
    }

    @Override
    public Opt<LibraryDef> createLibraryDialog(Path path, ImmutableList<Path> children, GamePlatform defaultPlatform) {
        log.info("Showing create library dialog...");
        final Opt<LibraryDef> libraryDef = stageManager.callWithBlur(() -> createLibraryDialog.show(path, children, defaultPlatform));
        if (libraryDef.isDefined()) {
            log.info("Library: {}", libraryDef.get());
        } else {
            log.info("Dialog cancelled.");
        }
        return libraryDef;
    }

    @Override
    public Opt<Path> addExcludedPathDialog() {
        return chooseDirectory("Add Excluded Path");
    }

    private Opt<Path> chooseDirectory(String title) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        directoryChooser.setInitialDirectory(configService.getPrevDirectory().map(Path::toFile).getOrElseNull());
        final File selectedDirectory = stageManager.callWithBlur(() -> directoryChooser.showDialog(stage));
        final Opt<Path> path = Opt.ofNullable(selectedDirectory).map(File::toURI).map(Paths::get);
        if (path.isDefined()) {
            configService.prevDirectoryProperty().set(path);
        }
        return path;
    }

    private Alert initAlert(Alert alert) {
        alert.initStyle(StageStyle.UTILITY);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(stage);
        return alert;
    }
}
