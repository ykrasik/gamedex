package com.github.ykrasik.gamedex.core.ui;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.NonNull;

import java.io.InputStream;
import java.net.URL;

/**
 * @author Yevgeny Krasik
 */
public final class UIResources {
    private UIResources() { }

    @Getter private static final Image notAvailable = new Image(getResourceAsStream("not_available.png"));
    @Getter private static final Image smallX = new Image(getResourceAsStream("x_small_icon.png"));

    @Getter private static final URL mainFxml = getResource("fxml/main.fxml");
    @Getter private static final String mainCss = "/com/github/ykrasik/gamedex/core/ui/css/main.css";

    public static Image getLogo() {
        return new Image(getResourceAsStream("gamedex.png"));
    }

    private static InputStream getResourceAsStream(String name) {
        @NonNull final InputStream resource = UIResources.class.getResourceAsStream(name);
        return resource;
    }

    private static URL getResource(String name) {
        @NonNull final URL resource = UIResources.class.getResource(name);
        return resource;
    }
}
