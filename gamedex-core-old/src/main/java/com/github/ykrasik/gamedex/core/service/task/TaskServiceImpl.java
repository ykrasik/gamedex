package com.github.ykrasik.gamedex.core.service.task;

import com.github.ykrasik.gamedex.core.service.AbstractService;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.yava.util.RunnableX;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.concurrent.Task;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class TaskServiceImpl extends AbstractService implements TaskService {
    @NonNull private final DialogService dialogService;

    private ExecutorService executorService;

    @Override
    protected void doStart() throws Exception {
        executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("worker-%d").build());
    }

    @Override
    protected void doStop() throws Exception {
        executorService.shutdownNow();
    }

    @Override
    public <V> Task<V> submit(Callable<V> callable) {
        final Task<V> task = createTask(callable);
        executorService.submit(task);
        return task;
    }

    @Override
    public <V> Task<V> submit(Callable<V> callable, Consumer<V> completionHandler) {
        final Task<V> task = createTask(callable);
        task.setOnSucceeded(e -> completionHandler.accept(task.getValue()));
        executorService.submit(task);
        return task;
    }

    @Override
    public Task<Void> submit(RunnableX runnable) {
        final Task<Void> task = createTask(runnable);
        executorService.submit(task);
        return task;
    }

    @Override
    public Task<Void> submit(RunnableX runnable, RunnableX completionHandler) {
        final Task<Void> task = createTask(runnable);
        task.setOnSucceeded(e -> completionHandler.run());
        executorService.submit(task);
        return task;
    }

    private <V> Task<V> createTask(Callable<V> callable) {
        final Task<V> task = new Task<V>() {
            @Override
            protected V call() throws Exception {
                return callable.call();
            }
        };
        task.setOnFailed(e -> dialogService.showException(task.getException()));
        return task;
    }

    private Task<Void> createTask(RunnableX runnable) {
        final Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                runnable.run();
                return null;
            }
        };
        task.setOnFailed(e -> dialogService.showException(task.getException()));
        return task;
    }
}
