package com.github.ykrasik.gamedex.core.manager.stage;

import com.github.ykrasik.gamedex.common.exception.RunnableThrows;
import javafx.stage.Stage;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public interface StageManager {
    void runWithBlur(RunnableThrows runnable);
    void runWithBlur(Stage stage, RunnableThrows runnable);

    <T> T callWithBlur(Callable<T> callable);
    <T> T callWithBlur(Stage stage, Callable<T> callable);
}
