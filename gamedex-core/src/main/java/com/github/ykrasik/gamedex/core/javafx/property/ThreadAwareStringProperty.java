package com.github.ykrasik.gamedex.core.javafx.property;

import com.github.ykrasik.yava.javafx.JavaFxUtils;
import javafx.beans.property.SimpleStringProperty;

/**
 * @author Yevgeny Krasik
 */
public class ThreadAwareStringProperty extends SimpleStringProperty {
    @Override
    protected void fireValueChangedEvent() {
        JavaFxUtils.runLaterIfNecessary(() -> super.fireValueChangedEvent());
    }
}
