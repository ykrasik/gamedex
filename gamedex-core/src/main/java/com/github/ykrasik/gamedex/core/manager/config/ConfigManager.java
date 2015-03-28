package com.github.ykrasik.gamedex.core.manager.config;

import javafx.beans.property.ObjectProperty;

/**
 * @author Yevgeny Krasik
 */
public interface ConfigManager {
    // TODO: DefaultReturnValue part of the API?
    <T> ObjectProperty<T> property(ConfigType type);
}
