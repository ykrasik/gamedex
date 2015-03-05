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
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

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
        this.settingsScreen = JavaFxUtils.returnLaterIfNecessary(() -> new SettingsScreen(configService));
        this.gameDetailScreen = JavaFxUtils.returnLaterIfNecessary(() -> new GameDetailScreen(imageService));
    }

    @Override
    @SneakyThrows
    public void doWithBlur(RunnableThrows runnable) {
        // Dialog must be displayed on JavaFx thread.
        final FutureTask<Void> futureTask = new FutureTask<>(() -> runWithBlur(runnable));
        JavaFxUtils.runLaterIfNecessary(futureTask);
        futureTask.get();
    }

    @Override
    @SneakyThrows
    public <T> T doWithBlur(Callable<T> callable) {
        // Dialog must be displayed on JavaFx thread.
        final FutureTask<T> futureTask = new FutureTask<>(() -> callWithBlur(callable));
        JavaFxUtils.runLaterIfNecessary(futureTask);
        return futureTask.get();
    }

    @Override
    public Opt<Game> showGameDetails(Game game) {
        return gameDetailScreen.show(game);
    }

    @Override
    public void showSettingsScreen() {
        doWithBlur(settingsScreen::show);
    }

    private <T> T callWithBlur(Callable<T> callable) throws Exception {
        final Scene scene = stage.getScene();
        if (scene != null) {
            scene.getRoot().setEffect(new GaussianBlur());
        }

        try {
            return callable.call();
        } finally {
            if (scene != null) {
                scene.getRoot().setEffect(null);
            }
        }
    }

    private Void runWithBlur(RunnableThrows runnable) throws Exception {
        return callWithBlur(() -> {
            runnable.run();
            return null;
        });
    }
}
