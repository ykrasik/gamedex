package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.core.service.dialog.choice.DialogChoice;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface DialogService {
    void showException(Throwable t);
    boolean confirmationDialog(String text);

    Opt<LibraryDef> addLibraryDialog(Opt<Path> initialDirectory);
    Opt<LibraryDef> createLibraryDialog(Path path, ImmutableList<Path> children, GamePlatform defaultPlatform);

    DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params);

    DialogChoice multipleSearchResultsDialog(MultipleSearchResultsDialogParams params);
}
