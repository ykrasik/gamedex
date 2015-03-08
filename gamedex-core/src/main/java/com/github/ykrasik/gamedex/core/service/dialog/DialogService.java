package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.collections.ObservableList;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface DialogService {
    void showException(Throwable t);
    boolean confirmationDialog(String format, Object... args);
    <T> boolean confirmationListDialog(ObservableList<T> list, String format, Object... args);

    Opt<LibraryDef> addLibraryDialog(Opt<Path> initialDirectory);
    Opt<LibraryDef> createLibraryDialog(Path path, ImmutableList<Path> children, GamePlatform defaultPlatform);
}
