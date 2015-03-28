package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.ui.library.CreateLibraryDialog;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;
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

/**
 * @author Yevgeny Krasik
 */
@Slf4j
public class DialogServiceImpl implements DialogService {
    private final Stage stage;
    private final StageManager stageManager;

    private final CreateLibraryDialog createLibraryDialog;

    public DialogServiceImpl(@NonNull Stage stage, @NonNull StageManager stageManager) {
        this.stage = stage;
        this.stageManager = stageManager;

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
    public <T> boolean confirmationListDialog(ObservableList<T> list, String format, Object... args) {
        final Alert alert = initAlert(DialogFactory.createConfirmationListDialog(String.format(format, args), list));
        final Optional<ButtonType> result = stageManager.callWithBlur(alert::showAndWait);
        return (result.get() == ButtonType.OK);
    }

    @Override
    public Opt<LibraryDef> addLibraryDialog(Opt<Path> initialDirectory) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Add Library");
        directoryChooser.setInitialDirectory(initialDirectory.map(Path::toFile).getOrElseNull());
        final File selectedDirectory = stageManager.callWithBlur(() -> directoryChooser.showDialog(stage));
        return Opt.ofNullable(selectedDirectory).flatMap(this::createLibraryFromFile);
    }

    @SneakyThrows
    private Opt<LibraryDef> createLibraryFromFile(File file) {
        final Path path = Paths.get(file.toURI());
        final ImmutableList<Path> children = FileUtils.listFirstChildDirectories(path, 10).newWith(Paths.get("..."));
        return createLibraryDialog(path, children, GamePlatform.PC);
    }

    @Override
    public Opt<LibraryDef> createLibraryDialog(Path path, ImmutableList<Path> children, GamePlatform defaultPlatform) {
        log.info("Showing create library dialog...");
        final Opt<LibraryDef> libraryDef = stageManager.callWithBlur(() -> createLibraryDialog.show(path, children, defaultPlatform));
        if (libraryDef.isPresent()) {
            log.info("Library: {}", libraryDef.get());
        } else {
            log.info("Dialog cancelled.");
        }
        return libraryDef;
    }

    private Alert initAlert(Alert alert) {
        alert.initStyle(StageStyle.UTILITY);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(stage);
        return alert;
    }
}
