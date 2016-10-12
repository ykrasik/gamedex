package com.github.ykrasik.gamedex.core.javafx.property;

import com.github.ykrasik.yava.javafx.JavaFxUtils;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * @author Yevgeny Krasik
 */
public class ThreadAwareDoubleProperty extends SimpleDoubleProperty {
    @Override
    protected void fireValueChangedEvent() {
        JavaFxUtils.runLaterIfNecessary(() -> super.fireValueChangedEvent());
    }
}
