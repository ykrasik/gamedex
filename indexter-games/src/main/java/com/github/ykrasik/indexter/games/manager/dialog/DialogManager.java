package com.github.ykrasik.indexter.games.manager.dialog;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.manager.dialog.choice.DialogChoice;

import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface DialogManager {
    boolean shouldCreateLibraryDialog(Path path) throws Exception;
    Optional<String> libraryNameDialog(Path path, GamePlatform platform) throws Exception;

    DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params);

    DialogChoice multipleSearchResultsDialog(MultipleSearchResultsDialogParams params);
}
