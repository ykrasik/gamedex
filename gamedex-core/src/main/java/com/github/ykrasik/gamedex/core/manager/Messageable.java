package com.github.ykrasik.gamedex.core.manager;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import javafx.beans.property.StringProperty;

/**
 * @author Yevgeny Krasik
 */
public interface Messageable {
    StringProperty messageProperty();

    default void message(String format, Object... args) {
        message(String.format(format, args));
    }

    default void message(String message) {
        messageProperty().set(message);
    }

    default void assertNotStopped() {
        if (Thread.interrupted()) {
            message("Stopping...");
            throw new GameDexException("Stopped.");
        }
    }
}
