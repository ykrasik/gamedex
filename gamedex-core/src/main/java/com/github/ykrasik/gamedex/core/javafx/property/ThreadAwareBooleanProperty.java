package com.github.ykrasik.gamedex.core.javafx.property;

import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * @author Yevgeny Krasik
 */
public class ThreadAwareBooleanProperty extends SimpleBooleanProperty {
    @Override
    protected void fireValueChangedEvent() {
        JavaFxUtils.runLaterIfNecessary(() -> super.fireValueChangedEvent());
    }
}
