package com.github.ykrasik.gamedex.core.ui.tableview;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.TableColumn;

/**
 * @author Yevgeny Krasik
 */
public class PercentageTableColumn<S, T> extends TableColumn<S, T> {
    private final DoubleProperty percentageWidth = new SimpleDoubleProperty(1);

    public PercentageTableColumn() {
        tableViewProperty().addListener((observable, oldValue, newValue) -> {
            if (prefWidthProperty().isBound()) {
                prefWidthProperty().unbind();
            }

            prefWidthProperty().bind(newValue.widthProperty().multiply(percentageWidth));
        });
    }

    public final DoubleProperty percentageWidthProperty() {
        return this.percentageWidth;
    }

    public final double getPercentageWidth() {
        return this.percentageWidthProperty().get();
    }

    public final void setPercentageWidth(double value) throws IllegalArgumentException {
        if (value >= 0 && value <= 1) {
            this.percentageWidthProperty().set(value);
        } else {
            throw new IllegalArgumentException(String.format("The provided percentage width is not between 0.0 and 1.0. Value is: %1$s", value));
        }
    }
}
