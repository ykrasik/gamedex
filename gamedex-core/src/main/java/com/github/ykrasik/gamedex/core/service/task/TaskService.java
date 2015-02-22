package com.github.ykrasik.gamedex.core.service.task;

import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * @author Yevgeny Krasik
 */
public interface TaskService {
    <V> Task<V> submit(Callable<V> callable);
    <V> Task<V> submit(Callable<V> callable, Consumer<V> completionHandler);

    Task<Void> submit(RunnableThrows runnable);
    Task<Void> submit(RunnableThrows runnable, RunnableThrows completionHandler);
}
