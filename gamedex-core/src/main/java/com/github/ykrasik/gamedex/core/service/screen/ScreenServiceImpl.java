package com.github.ykrasik.gamedex.core.service.screen;

import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import com.github.ykrasik.gamedex.core.config.ConfigService;
import com.github.ykrasik.gamedex.core.javafx.JavaFxUtils;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.service.screen.detail.GameDetailScreen;
import com.github.ykrasik.gamedex.core.service.screen.settings.SettingsScreen;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.opt.Opt;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import lombok.NonNull;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public class ScreenServiceImpl implements ScreenService {
    private final Stage stage;
    private final SettingsScreen settingsScreen;
    private final GameDetailScreen gameDetailScreen;

    public ScreenServiceImpl(@NonNull Stage stage,
                             @NonNull ConfigService configService,
                             @NonNull ImageService imageService) {
        this.stage = stage;
        this.settingsScreen = JavaFxUtils.callLaterIfNecessary(() -> new SettingsScreen(configService));
        this.gameDetailScreen = JavaFxUtils.callLaterIfNecessary(() -> new GameDetailScreen(imageService));
    }

    @Override
    public void runWithBlur(RunnableThrows runnable) {
        callWithBlur(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <T> T callWithBlur(Callable<T> callable) {
        final Scene scene = stage.getScene();
        if (scene != null) {
            scene.getRoot().setEffect(new GaussianBlur());
        }

        try {
            return JavaFxUtils.callLaterIfNecessary(callable);
        } finally {
            if (scene != null) {
                scene.getRoot().setEffect(null);
            }
        }
    }

    @Override
    public Opt<Game> showGameDetails(Game game) {
        return gameDetailScreen.show(game);
    }

    @Override
    public void showSettingsScreen() {
        runWithBlur(settingsScreen::show);
    }
}
