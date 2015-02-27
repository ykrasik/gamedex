package com.github.ykrasik.gamedex.core.config;

import javafx.beans.property.ObjectProperty;

/**
 * @author Yevgeny Krasik
 */
public interface ConfigService {
    // TODO: DefaultReturnValue part of the API?
    <T> ObjectProperty<T> property(ConfigType type);
}
