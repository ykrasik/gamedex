package com.github.ykrasik.gamedex.core.ui;

import javafx.scene.image.Image;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public final class UIResources {
    private UIResources() { }

    private static final String BASE_PATH = "/com/github/ykrasik/gamedex/core/ui/";
    private static final String CSS_PATH = BASE_PATH + "css/";
    private static final String IMAGE_PATH = BASE_PATH + "image/";

    @Getter private static final Image notAvailable = new Image(getResourceAsStream(IMAGE_PATH + "not_available.png"));
    @Getter private static final Image imageLoading = new Image(getResourceAsStream(IMAGE_PATH + "loading_spinner.gif"));

    public static Image getLogo() {
        return new Image(getResourceAsStream(IMAGE_PATH + "gamedex.png"));
    }

    @Getter private static final URL mainFxml = getResource("fxml/main.fxml");
    @Getter private static final URL libraryDialogFxml = getResource("fxml/libraryDialog.fxml");
    @Getter private static final URL detailViewDialogFxml = getResource("fxml/gameDetailView.fxml");

    @Getter private static final String mainCss = CSS_PATH + "main.css";
    @Getter private static final String libraryDialogCss = CSS_PATH + "libraryDialog.css";
    @Getter private static final String detailViewDialogCss = CSS_PATH + "gameDetailView.css";

    private static InputStream getResourceAsStream(String name) {
        return Objects.requireNonNull(UIResources.class.getResourceAsStream(name), "Can't find resource on classpath: " + name);
    }

    private static URL getResource(String name) {
        return Objects.requireNonNull(UIResources.class.getResource(name), "Can't find resource on classpath: " + name);
    }
}
