package com.github.ykrasik.gamedex.common.preloader;

import com.github.ykrasik.gamedex.common.exception.ConsumerThrows;
import javafx.concurrent.Task;

/**
 * @author Yevgeny Krasik
 */
public interface Preloader {
    void info(String message);

    <T> void start(Task<T> task, ConsumerThrows<T> consumer);
}
