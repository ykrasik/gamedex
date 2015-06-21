package com.github.ykrasik.gamedex.core.manager.stage;

import com.github.ykrasik.yava.util.RunnableX;
import javafx.stage.Stage;

import java.util.concurrent.Callable;

/**
 * @author Yevgeny Krasik
 */
public interface StageManager {
    void runWithBlur(RunnableX runnable);
    void runWithBlur(Stage stage, RunnableX runnable);

    <T> T callWithBlur(Callable<T> callable);
    <T> T callWithBlur(Stage stage, Callable<T> callable);
}
