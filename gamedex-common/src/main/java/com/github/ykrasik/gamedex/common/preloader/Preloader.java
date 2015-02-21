package com.github.ykrasik.gamedex.common.preloader;

import javafx.concurrent.Task;

/**
 * @author Yevgeny Krasik
 */
public interface Preloader {
    void message(String message);

    <T> void start(Task<T> task, Runnable runnable);
}
