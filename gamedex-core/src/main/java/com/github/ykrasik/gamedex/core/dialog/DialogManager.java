package com.github.ykrasik.gamedex.core.dialog;

import com.github.ykrasik.gamedex.core.dialog.choice.DialogChoice;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.opt.Opt;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface DialogManager {
    boolean shouldCreateLibraryDialog(Path path) throws Exception;
    Opt<String> libraryNameDialog(Path path, GamePlatform platform) throws Exception;

    DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params);

    DialogChoice multipleSearchResultsDialog(MultipleSearchResultsDialogParams params);
}
