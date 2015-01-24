package com.github.ykrasik.indexter.games.manager.scan;

import com.github.ykrasik.indexter.games.datamodel.LocalLibrary;
import org.controlsfx.control.StatusBar;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface ScanManager {
    void refreshLibraries(ExceptionHandler exceptionHandler);

    void processPath(LocalLibrary library, Path path, ExceptionHandler exceptionHandler);

    // FIXME: UGH! This is not where it belongs. Do this through fxml.
    StatusBar getStatusBar();
}
