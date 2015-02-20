package com.github.ykrasik.gamedex.core.dialog;

import com.github.ykrasik.gamedex.core.dialog.choice.DialogChoice;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
public interface DialogManager {
    void showException(Throwable t);

    Opt<LibraryDef> addLibraryDialog(Opt<Path> initialDirectory);
    Opt<LibraryDef> createLibraryDialog(Path path, List<Path> children, GamePlatform defaultPlatform);

    DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params);

    DialogChoice multipleSearchResultsDialog(MultipleSearchResultsDialogParams params);
}
