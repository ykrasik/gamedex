package com.github.ykrasik.gamedex.provider;

import javafx.scene.image.Image;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class GameInfoProviderInfo {
    @NonNull private final String name;
    private final boolean required;
    @NonNull private final Image logo;
}
