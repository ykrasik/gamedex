package com.github.ykrasik.gamedex.common.util;

import javafx.application.Platform;

/**
 * @author Yevgeny Krasik
 */
public final class PlatformUtils {
    private PlatformUtils() {
    }

    public static void runLaterIfNecessary(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }
}
