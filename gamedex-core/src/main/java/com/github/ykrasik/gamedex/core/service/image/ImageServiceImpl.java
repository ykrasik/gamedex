package com.github.ykrasik.gamedex.core.service.image;

import com.github.ykrasik.gamedex.common.service.AbstractService;
import com.github.ykrasik.gamedex.common.util.JavaFxUtils;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.gamedex.persistence.PersistenceService;
import com.github.ykrasik.opt.Opt;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import lombok.Lombok;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ImageServiceImpl extends AbstractService implements ImageService {
    private static final Duration FADE_IN_DURATION = Duration.seconds(0.02);
    private static final Duration FADE_OUT_DURATION = Duration.seconds(0.05);
    private static final boolean SYNC = false;   // Debug

    @NonNull private final PersistenceService persistenceService;

    private ExecutorService executorService;

    @Override
    protected void doStart() throws Exception {
        // FIXME: There is a weird race condition with the thumbnails when more than 1 thread.
        executorService = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("image-fetcher-%d").build());
    }

    @Override
    protected void doStop() throws Exception {
        executorService.shutdownNow();
    }

    @Override
    public Task<Image> fetchThumbnail(Id<Game> id, ImageView imageView) {
        return createLoadingTask(() -> persistenceService.getThumbnail(id), imageView);
    }

    @Override
    public Task<Image> fetchPoster(Id<Game> id, ImageView imageView) {
        return createLoadingTask(() -> {
            final Opt<ImageData> poster = persistenceService.getPoster(id);
            if (poster.isEmpty()) {
                LOG.info("No poster for gameId={}, returning thumbnail...", id);
                return persistenceService.getThumbnail(id);
            } else {
                return poster;
            }
        }, imageView);
    }

    private Task<Image> createLoadingTask(Callable<Opt<ImageData>> fetcher, ImageView imageView) {
        final Task<Image> task = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                return fetcher.call().map(ImageData::getImage).getOrElse(UIResources.notAvailable());
            }
        };

        task.setOnFailed(e -> Lombok.sneakyThrow(task.getException()));

        // Set imageView to a "loading" image.
        JavaFxUtils.runLaterIfNecessary(() -> imageView.setImage(UIResources.imageLoading()));

        final FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, imageView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Prepare a fade transition to the loaded image.
        final FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, imageView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            JavaFxUtils.runLaterIfNecessary(() -> imageView.setImage(task.getValue()));
            fadeIn.play();
        });

        task.setOnSucceeded(e -> fadeOut.play());

        if (SYNC) {
            // Debug
            return performTask(task);
        } else {
            final Future<?> future = executorService.submit(task);
            task.setOnCancelled(e -> future.cancel(true));
            return task;
        }
    }

    @SneakyThrows
    private Task<Image> performTask(Task<Image> task) {
        task.run();
        return task;
    }
}
