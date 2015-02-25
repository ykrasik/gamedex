package com.github.ykrasik.gamedex.core.service.screen;

import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import com.github.ykrasik.gamedex.common.util.JavaFxUtils;
import com.github.ykrasik.gamedex.core.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.detail.GameDetailScreen;
import com.github.ykrasik.gamedex.core.ui.settings.SettingsScreen;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.opt.Opt;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

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
        this.settingsScreen = new SettingsScreen(configService);
        this.gameDetailScreen = new GameDetailScreen(imageService);
    }

    @Override
    @SneakyThrows
    public void doWithBlur(RunnableThrows runnable) {
        // Dialog must be displayed on JavaFx thread.
        final FutureTask<Void> futureTask = new FutureTask<>(() -> runWithBlur(runnable));
        JavaFxUtils.runLaterIfNecessary(futureTask);
        futureTask.get(10, TimeUnit.SECONDS);
    }

    @Override
    @SneakyThrows
    public <T> T doWithBlur(Callable<T> callable) {
        // Dialog must be displayed on JavaFx thread.
        final FutureTask<T> futureTask = new FutureTask<>(() -> callWithBlur(callable));
        JavaFxUtils.runLaterIfNecessary(futureTask);
        return futureTask.get(10, TimeUnit.SECONDS);
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
        final Parent root = stage.getScene().getRoot();
        root.setEffect(new GaussianBlur());
        try {
            return callable.call();
        } finally {
            root.setEffect(null);
        }
    }

    private Void runWithBlur(RunnableThrows runnable) throws Exception {
        final Parent root = stage.getScene().getRoot();
        root.setEffect(new GaussianBlur());
        try {
            runnable.run();
        } finally {
            root.setEffect(null);
        }
        return null;
    }
}
