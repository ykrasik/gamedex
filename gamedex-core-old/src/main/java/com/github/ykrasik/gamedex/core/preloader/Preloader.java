package com.github.ykrasik.gamedex.core.preloader;

import javafx.concurrent.Task;

import java.util.function.Consumer;

/**
 * @author Yevgeny Krasik
 */
public interface Preloader {
    void message(String message);

    <T> void start(Task<T> task, Consumer<T> onFinished);
}
